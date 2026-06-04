package squirrelvault.repository

import squirrelvault.domain.BackupJob
import zio.*

trait BackupJobRepository:
  def save(job: BackupJob): Task[BackupJob]
  def findAll(enabled: Option[Boolean]): Task[List[BackupJob]]
  def findById(id: String): Task[Option[BackupJob]]
  def disable(id: String): Task[Option[BackupJob]]
  def enable(id: String): Task[Option[BackupJob]]

object BackupJobRepository:
  def save(job: BackupJob): RIO[BackupJobRepository, BackupJob] =
    ZIO.serviceWithZIO(_.save(job))
  def findAll(enabled: Option[Boolean]): RIO[BackupJobRepository, List[BackupJob]] =
    ZIO.serviceWithZIO(_.findAll(enabled))
  def findById(id: String): RIO[BackupJobRepository, Option[BackupJob]] =
    ZIO.serviceWithZIO(_.findById(id))
  def disable(id: String): RIO[BackupJobRepository, Option[BackupJob]] =
    ZIO.serviceWithZIO(_.disable(id))
  def enable(id: String): RIO[BackupJobRepository, Option[BackupJob]] =
    ZIO.serviceWithZIO(_.enable(id))

final class InMemoryBackupJobRepository(ref: Ref[Map[String, BackupJob]]) extends BackupJobRepository:
  def save(job: BackupJob): Task[BackupJob] =
    ref.update(_.updated(job.id, job)).as(job)

  def findAll(enabled: Option[Boolean]): Task[List[BackupJob]] =
    ref.get.map { jobs =>
      val all = jobs.values.toList
      enabled.fold(all)(e => all.filter(_.enabled == e))
    }

  def findById(id: String): Task[Option[BackupJob]] =
    ref.get.map(_.get(id))

  def disable(id: String): Task[Option[BackupJob]] =
    ref.modify { jobs =>
      jobs.get(id) match
        case None      => (None, jobs)
        case Some(job) =>
          val updated = job.copy(enabled = false)
          (Some(updated), jobs.updated(id, updated))
    }

  def enable(id: String): Task[Option[BackupJob]] =
    ref.modify { jobs =>
      jobs.get(id) match
        case None      => (None, jobs)
        case Some(job) =>
          val updated = job.copy(enabled = true)
          (Some(updated), jobs.updated(id, updated))
    }

object InMemoryBackupJobRepository:
  val layer: ULayer[BackupJobRepository] =
    ZLayer.fromZIO(Ref.make(Map.empty[String, BackupJob]).map(new InMemoryBackupJobRepository(_)))
