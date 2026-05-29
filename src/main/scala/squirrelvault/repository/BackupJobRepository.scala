package squirrelvault.repository

import squirrelvault.domain.BackupJob
import zio.*

trait BackupJobRepository:
  def save(job: BackupJob): UIO[BackupJob]
  def findAll(enabled: Option[Boolean]): UIO[List[BackupJob]]
  def findById(id: String): UIO[Option[BackupJob]]
  def disable(id: String): UIO[Option[BackupJob]]

object BackupJobRepository:
  def save(job: BackupJob): URIO[BackupJobRepository, BackupJob] =
    ZIO.serviceWithZIO(_.save(job))
  def findAll(enabled: Option[Boolean]): URIO[BackupJobRepository, List[BackupJob]] =
    ZIO.serviceWithZIO(_.findAll(enabled))
  def findById(id: String): URIO[BackupJobRepository, Option[BackupJob]] =
    ZIO.serviceWithZIO(_.findById(id))
  def disable(id: String): URIO[BackupJobRepository, Option[BackupJob]] =
    ZIO.serviceWithZIO(_.disable(id))

final class InMemoryBackupJobRepository(ref: Ref[Map[String, BackupJob]]) extends BackupJobRepository:
  def save(job: BackupJob): UIO[BackupJob] =
    ref.update(_.updated(job.id, job)).as(job)

  def findAll(enabled: Option[Boolean]): UIO[List[BackupJob]] =
    ref.get.map { jobs =>
      val all = jobs.values.toList
      enabled.fold(all)(e => all.filter(_.enabled == e))
    }

  def findById(id: String): UIO[Option[BackupJob]] =
    ref.get.map(_.get(id))

  def disable(id: String): UIO[Option[BackupJob]] =
    ref.modify { jobs =>
      jobs.get(id) match
        case None => (None, jobs)
        case Some(job) =>
          val updated = job.copy(enabled = false)
          (Some(updated), jobs.updated(id, updated))
    }

object InMemoryBackupJobRepository:
  val layer: ULayer[BackupJobRepository] =
    ZLayer.fromZIO(Ref.make(Map.empty[String, BackupJob]).map(new InMemoryBackupJobRepository(_)))
