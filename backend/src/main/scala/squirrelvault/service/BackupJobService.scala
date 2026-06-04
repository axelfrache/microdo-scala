package squirrelvault.service

import squirrelvault.api.dto.{CreateBackupJobRequest, ValidationErrorResponse}
import squirrelvault.domain.BackupJob
import squirrelvault.repository.BackupJobRepository
import squirrelvault.validation.BackupJobValidator
import zio.*
import java.time.Instant
import java.util.UUID

trait BackupJobService:
  def create(req: CreateBackupJobRequest): IO[ValidationErrorResponse, BackupJob]
  def list(enabled: Option[Boolean]): Task[List[BackupJob]]
  def findById(id: String): Task[Option[BackupJob]]
  def disable(id: String): Task[Option[BackupJob]]
  def enable(id: String): Task[Option[BackupJob]]

object BackupJobService:
  def create(req: CreateBackupJobRequest): ZIO[BackupJobService, ValidationErrorResponse, BackupJob] =
    ZIO.serviceWithZIO(_.create(req))
  def list(enabled: Option[Boolean]): RIO[BackupJobService, List[BackupJob]] =
    ZIO.serviceWithZIO(_.list(enabled))
  def findById(id: String): RIO[BackupJobService, Option[BackupJob]] =
    ZIO.serviceWithZIO(_.findById(id))
  def disable(id: String): RIO[BackupJobService, Option[BackupJob]] =
    ZIO.serviceWithZIO(_.disable(id))
  def enable(id: String): RIO[BackupJobService, Option[BackupJob]] =
    ZIO.serviceWithZIO(_.enable(id))

final class BackupJobServiceImpl(repo: BackupJobRepository) extends BackupJobService:
  def create(req: CreateBackupJobRequest): IO[ValidationErrorResponse, BackupJob] =
    val errors = BackupJobValidator.validate(req)
    if errors.nonEmpty then ZIO.fail(ValidationErrorResponse(errors))
    else
      val job = BackupJob(
        id = UUID.randomUUID().toString,
        name = req.name,
        sourceType = req.sourceType,
        source = req.source,
        targetBucket = req.targetBucket,
        targetPrefix = req.targetPrefix,
        schedule = req.schedule,
        retentionDays = req.retentionDays,
        critical = req.critical,
        expectedFrequencyHours = req.expectedFrequencyHours,
        enabled = req.enabled,
        createdAt = Instant.now()
      )
      repo.save(job).mapError(e => ValidationErrorResponse(List(e.getMessage)))

  def list(enabled: Option[Boolean]): Task[List[BackupJob]] = repo.findAll(enabled)
  def findById(id: String): Task[Option[BackupJob]]         = repo.findById(id)
  def disable(id: String): Task[Option[BackupJob]]          = repo.disable(id)
  def enable(id: String): Task[Option[BackupJob]]           = repo.enable(id)

object BackupJobServiceImpl:
  val layer: URLayer[BackupJobRepository, BackupJobService] =
    ZLayer.fromFunction(new BackupJobServiceImpl(_))
