package squirrelvault.service

import squirrelvault.domain.BackupRun
import squirrelvault.repository.BackupRunRepository
import zio.*
import java.time.Instant
import java.util.UUID

trait BackupExecutor:
  def startRun(jobId: String, runId: String): Task[Unit]

object BackupExecutor:
  def startRun(jobId: String, runId: String): ZIO[BackupExecutor, Throwable, Unit] = ZIO.serviceWithZIO(_.startRun(jobId, runId))

/**
 * A trivial executor that simulates a dump by sleeping and then marking SUCCESS.
 * Replace with real pg_dump + upload logic later.
 */
final class SimpleBackupExecutor(repo: BackupRunRepository) extends BackupExecutor:
  def startRun(jobId: String, runId: String): Task[Unit] =
    val now = Instant.now()
    val running = BackupRun(runId, jobId, "RUNNING", Some(now), None, None, None, None)
    for
      _ <- repo.update(running)
      // simulate work
      _ <- ZIO.sleep(2.seconds)
      finished = Instant.now()
      completed = running.copy(status = "SUCCESS", finishedAt = Some(finished), sizeMb = Some(123), location = Some(s"s3://backups/$jobId/$runId.dump"))
      _ <- repo.update(completed)
    yield ()

object SimpleBackupExecutor:
  val layer: URLayer[BackupRunRepository, BackupExecutor] = ZLayer.fromFunction(new SimpleBackupExecutor(_))
