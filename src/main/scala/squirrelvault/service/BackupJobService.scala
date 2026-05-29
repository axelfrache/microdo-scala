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
  def list(enabled: Option[Boolean]): UIO[List[BackupJob]]
  def findById(id: String): UIO[Option[BackupJob]]
  def disable(id: String): UIO[Option[BackupJob]]

object BackupJobService:
  def create(req: CreateBackupJobRequest): ZIO[BackupJobService, ValidationErrorResponse, BackupJob] =
    ZIO.serviceWithZIO(_.create(req))
  def list(enabled: Option[Boolean]): URIO[BackupJobService, List[BackupJob]] =
    ZIO.serviceWithZIO(_.list(enabled))
  def findById(id: String): URIO[BackupJobService, Option[BackupJob]] =
    ZIO.serviceWithZIO(_.findById(id))
  def disable(id: String): URIO[BackupJobService, Option[BackupJob]] =
    ZIO.serviceWithZIO(_.disable(id))

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
      repo.save(job)

  def list(enabled: Option[Boolean]): UIO[List[BackupJob]] = repo.findAll(enabled)
  def findById(id: String): UIO[Option[BackupJob]]         = repo.findById(id)
  def disable(id: String): UIO[Option[BackupJob]]          = repo.disable(id)

object BackupJobServiceImpl:
  val layer: URLayer[BackupJobRepository, BackupJobService] =
    ZLayer.fromFunction(new BackupJobServiceImpl(_))
