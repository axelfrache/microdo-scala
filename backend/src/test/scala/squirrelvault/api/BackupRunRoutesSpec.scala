package squirrelvault.api

import squirrelvault.domain.{BackupJob, RunStatus, SourceType}
import squirrelvault.repository.InMemoryBackupRunRepository
import squirrelvault.repository.InMemoryBackupJobRepository
import squirrelvault.repository.BackupJobRepository
import squirrelvault.service.BackupExecutor
import squirrelvault.service.BackupExecutorService
import zio.*
import squirrelvault.repository.BackupRunRepository
import squirrelvault.service.BackupRunDispatcher
import squirrelvault.service.InMemoryBackupRunDispatcher
import squirrelvault.service.BackupStorageConfig
import zio.telemetry.opentelemetry.tracing.Tracing
import squirrelvault.telemetry.Telemetry
import zio.http.*
import zio.json.*
import zio.test.*
import java.time.Instant

final case class StartRunResponse(runId: String, status: String)

object StartRunResponse:
  given JsonCodec[StartRunResponse] = DeriveJsonCodec.gen[StartRunResponse]

object BackupRunRoutesSpec extends ZIOSpecDefault:

  private val mockExecutorLayer = ZLayer.fromFunction { (repo: BackupRunRepository) =>
    new BackupExecutorService:
      def startRun(jobId: String, runId: String): Task[Unit] =
        for
          now <- Clock.instant
          _ <- repo.update(
            squirrelvault.domain
              .BackupRun(
                runId,
                jobId,
                RunStatus.Success,
                Some(now),
                Some(now),
                Some(0),
                Some(10),
                Some("s3://dummy"),
                None
              )
          )
        yield ()
  }

  private val runLayer =
    ZLayer.make[BackupJobRepository & BackupRunRepository & BackupExecutorService & BackupRunDispatcher & Tracing](
      InMemoryBackupJobRepository.layer,
      InMemoryBackupRunRepository.layer,
      mockExecutorLayer,
      InMemoryBackupRunDispatcher.layer,
      Telemetry.layer
    )

  private val seedJob = BackupJob(
    id = "myjob",
    name = "questify-postgres-daily",
    sourceType = SourceType.PostgreSQL,
    source = "postgres://questify-db",
    targetBucket = "squirrelvault-backups",
    targetPrefix = "questify/postgres",
    schedule = "0 2 * * *",
    retentionDays = 14,
    critical = true,
    expectedFrequencyHours = 24,
    enabled = true,
    createdAt = Instant.parse("2026-06-03T00:00:00Z")
  )

  def spec = suite("BackupRunRoutes")(
    test("POST start and GET status") {
      ZIO.scoped {
        runLayer.build.flatMap { env =>
          for
            _ <- BackupJobRepository.save(seedJob).provideEnvironment(env)
            request = Request.post("/backup-jobs/myjob/run", Body.empty)
            response <- BackupRunRoutes.routes.runZIO(request).provideEnvironment(env)
            body <- response.body.asString
            startRun <- ZIO.fromEither(body.fromJson[StartRunResponse].left.map(error => new RuntimeException(error)))
            _ <- TestClock.adjust(3.seconds)
            statusResponse <- BackupRunRoutes.routes
              .runZIO(Request.get(URL(Path.decode(s"/backup-runs/${startRun.runId}"))))
              .provideEnvironment(env)
            statusBody <- statusResponse.body.asString
            run <- ZIO.fromEither(
              statusBody.fromJson[squirrelvault.domain.BackupRun].left.map(error => new RuntimeException(error))
            )
          yield assertTrue(
            response.status == Status.Accepted,
            startRun.status == "PENDING",
            statusResponse.status == Status.Ok,
            run.status == RunStatus.Success
          )
        }
      }
    }
  )
