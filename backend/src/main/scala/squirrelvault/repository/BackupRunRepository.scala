package squirrelvault.repository

import squirrelvault.domain.BackupRun
import zio.*

trait BackupRunRepository:
  def create(run: BackupRun): Task[BackupRun]
  def update(run: BackupRun): Task[BackupRun]
  def findById(id: String): Task[Option[BackupRun]]
  def listByJobId(jobId: String): Task[List[BackupRun]]

object BackupRunRepository:
  def create(run: BackupRun): RIO[BackupRunRepository, BackupRun] = ZIO.serviceWithZIO(_.create(run))
  def update(run: BackupRun): RIO[BackupRunRepository, BackupRun] = ZIO.serviceWithZIO(_.update(run))
  def findById(id: String): RIO[BackupRunRepository, Option[BackupRun]] = ZIO.serviceWithZIO(_.findById(id))
  def listByJobId(jobId: String): RIO[BackupRunRepository, List[BackupRun]] = ZIO.serviceWithZIO(_.listByJobId(jobId))

final class InMemoryBackupRunRepository(ref: Ref[Map[String, BackupRun]]) extends BackupRunRepository:
  def create(run: BackupRun): Task[BackupRun] = ref.update(_.updated(run.id, run)).as(run)

  def update(run: BackupRun): Task[BackupRun] = ref.update(_.updated(run.id, run)).as(run)

  def findById(id: String): Task[Option[BackupRun]] = ref.get.map(_.get(id))

  def listByJobId(jobId: String): Task[List[BackupRun]] = ref.get.map(_.values.filter(_.jobId == jobId).toList)

object InMemoryBackupRunRepository:
  val layer: ULayer[BackupRunRepository] =
    ZLayer.fromZIO(Ref.make(Map.empty[String, BackupRun]).map(new InMemoryBackupRunRepository(_)))
