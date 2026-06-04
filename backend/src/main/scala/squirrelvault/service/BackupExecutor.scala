package squirrelvault.service

import io.minio.{BucketExistsArgs, MakeBucketArgs, MinioClient, UploadObjectArgs}
import squirrelvault.domain.BackupRun
import squirrelvault.domain.{BackupJob, SourceType}
import squirrelvault.repository.{BackupJobRepository, BackupRunRepository}
import zio.*

import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.time.{Duration, Instant}

trait BackupExecutorService:
  def startRun(jobId: String, runId: String): Task[Unit]

object BackupExecutor:
  def startRun(jobId: String, runId: String): ZIO[BackupExecutorService, Throwable, Unit] =
    ZIO.serviceWithZIO(_.startRun(jobId, runId))

  val layer: RLayer[BackupJobRepository & BackupRunRepository & BackupStorageConfig, BackupExecutorService] =
    ZLayer {
      for
        jobRepository <- ZIO.service[BackupJobRepository]
        runRepository <- ZIO.service[BackupRunRepository]
        storageConfig <- ZIO.service[BackupStorageConfig]
      yield new BackupExecutor(jobRepository, runRepository, storageConfig)
    }

final class BackupExecutor(
    jobRepository: BackupJobRepository,
    runRepository: BackupRunRepository,
    storageConfig: BackupStorageConfig
) extends BackupExecutorService:

  def startRun(jobId: String, runId: String): Task[Unit] =
    for
      startedAt <- Clock.instant
      _ <- updateRun(runId, jobId, "RUNNING", Some(startedAt), None, None, None, None, None)
      outcome <- runBackup(jobId, runId, startedAt).catchAll { error =>
        for
          finishedAt <- Clock.instant
          durationMs = Duration.between(startedAt, finishedAt).toMillis
          _ <- updateRun(
            runId,
            jobId,
            "FAILED",
            Some(startedAt),
            Some(finishedAt),
            Some(durationMs),
            None,
            None,
            Some(error.getMessage)
          )
        yield ()
      }
    yield outcome

  private def runBackup(jobId: String, runId: String, startedAt: Instant): Task[Unit] =
    ZIO.scoped {
      for
        job <- jobRepository.findById(jobId).flatMap {
          case Some(job) => ZIO.succeed(job)
          case None      => ZIO.fail(new IllegalArgumentException(s"Backup job $jobId not found"))
        }
        _ <- validateJob(job)
        dumpFile <- ZIO.acquireRelease(ZIO.attemptBlocking(Files.createTempFile(s"backup-$runId-", ".dump")))(cleanupTempFile)
        _ <- runPgDump(job.source, dumpFile)
        sizeBytes <- ZIO.attemptBlocking(Files.size(dumpFile))
        location <- uploadToMinio(job, runId, dumpFile)
        finishedAt <- Clock.instant
        durationMs = Duration.between(startedAt, finishedAt).toMillis
        _ <- updateRun(
          runId,
          jobId,
          "SUCCESS",
          Some(startedAt),
          Some(finishedAt),
          Some(durationMs),
          Some(sizeBytes / (1024L * 1024L)),
          Some(location),
          None
        )
      yield ()
    }

  private def validateJob(job: BackupJob): Task[Unit] =
    job.sourceType match
      case SourceType.PostgreSQL => ZIO.unit
      case other                 => ZIO.fail(new IllegalArgumentException(s"Unsupported source type for backup: $other"))

  private def runPgDump(source: String, dumpFile: Path): Task[Unit] =
    ZIO.attemptBlocking {
      val connection = parsePostgresSource(source)
      val command = List(
        "pg_dump",
        "--format=custom",
        "--no-owner",
        "--no-acl",
        "--host",
        connection.host,
        "--username",
        connection.user,
        "--dbname",
        connection.database,
        "--file",
        dumpFile.toString
      ) ++ connection.port.map(port => List("--port", port.toString)).getOrElse(Nil)

      val processBuilder = new ProcessBuilder(command*)
      connection.password.foreach(password => processBuilder.environment().put("PGPASSWORD", password))
      val process = processBuilder.redirectErrorStream(true).start()

      val exitCode = process.waitFor()
      if exitCode != 0 then
        val output = new String(process.getInputStream.readAllBytes(), StandardCharsets.UTF_8)
        throw new RuntimeException(s"pg_dump failed with exit code $exitCode: $output")
    }

  private final case class PostgresConnection(host: String, port: Option[Int], user: String, password: Option[String], database: String)

  private def parsePostgresSource(source: String): PostgresConnection =
    val uri = URI.create(source)
    val scheme = Option(uri.getScheme).map(_.toLowerCase)
    val host = Option(uri.getHost).getOrElse("")
    val userInfo = Option(uri.getUserInfo)
    val path = Option(uri.getPath).getOrElse("")
    val database = if path.nonEmpty && path != "/" then path.stripPrefix("/") else host
    val (user, password) = userInfo match
      case Some(value) if value.contains(":") =>
        value.split(":", 2) match
          case Array(rawUser, rawPassword) => (rawUser, Some(rawPassword))
          case Array(rawUser)              => (rawUser, None)
      case Some(value) if value.nonEmpty => (value, None)
      case _                              => (host, None)

    if !scheme.exists(Set("postgres", "postgresql").contains) || host.isBlank || database.isBlank then
      throw new IllegalArgumentException(s"Unsupported PostgreSQL source URI: $source")

    PostgresConnection(
      host = host,
      port = Option(uri.getPort).filter(_ > 0),
      user = user,
      password = password,
      database = database
    )

  private def uploadToMinio(job: BackupJob, runId: String, dumpFile: Path): Task[String] =
    ZIO.attemptBlocking {
      val client = MinioClient.builder()
        .endpoint(storageConfig.endpoint)
        .credentials(storageConfig.accessKey, storageConfig.secretKey)
        .build()

      if !client.bucketExists(BucketExistsArgs.builder().bucket(job.targetBucket).build()) then
        client.makeBucket(MakeBucketArgs.builder().bucket(job.targetBucket).build())

      val objectName = buildObjectName(job.targetPrefix, runId)
      client.uploadObject(
        UploadObjectArgs.builder()
          .bucket(job.targetBucket)
          .`object`(objectName)
          .filename(dumpFile.toString)
          .build()
      )
      s"s3://${job.targetBucket}/$objectName"
    }

  private def buildObjectName(prefix: String, runId: String): String =
    val cleanPrefix = prefix.stripSuffix("/")
    if cleanPrefix.isBlank then s"$runId.dump" else s"$cleanPrefix/$runId.dump"

  private def updateRun(
      runId: String,
      jobId: String,
      status: String,
      startedAt: Option[Instant],
      finishedAt: Option[Instant],
      duration: Option[Long],
      sizeMb: Option[Long],
      location: Option[String],
      error: Option[String]
  ): Task[BackupRun] =
    val existing = BackupRun(runId, jobId, status, startedAt, finishedAt, duration, sizeMb, location, error)
    runRepository.update(existing)

  private def cleanupTempFile(file: Path): UIO[Unit] =
    ZIO.attemptBlocking(Files.deleteIfExists(file)).ignore
