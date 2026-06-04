package squirrelvault.service

import zio.*

trait BackupRunDispatcher:
  def enqueue(jobId: String, runId: String): Task[Unit]

object BackupRunDispatcher:
  def enqueue(jobId: String, runId: String): RIO[BackupRunDispatcher, Unit] =
    ZIO.serviceWithZIO(_.enqueue(jobId, runId))

final class InMemoryBackupRunDispatcher(queue: Queue[(String, String)]) extends BackupRunDispatcher:
  def enqueue(jobId: String, runId: String): Task[Unit] = queue.offer((jobId, runId)).unit

object InMemoryBackupRunDispatcher:
  val layer: URLayer[BackupExecutorService, BackupRunDispatcher] =
    ZLayer.scoped {
      for
        queue <- Queue.unbounded[(String, String)]
        executor <- ZIO.service[BackupExecutorService]
        fiber <- queue.take
          .flatMap { case (jobId, runId) =>
            executor
              .startRun(jobId, runId)
              .catchAllCause(cause => ZIO.logErrorCause(s"Backup run failed for job=$jobId run=$runId", cause))
          }
          .forever
          .forkDaemon
        _ <- ZIO.addFinalizer(fiber.interrupt)
      yield new InMemoryBackupRunDispatcher(queue)
    }
