package squirrelvault.service

import squirrelvault.api.dto.CreateBackupJobRequest
import squirrelvault.domain.SourceType
import squirrelvault.repository.InMemoryBackupJobRepository
import zio.*
import zio.test.*

object BackupJobServiceSpec extends ZIOSpecDefault:

  private val serviceLayer: ULayer[BackupJobService] =
    InMemoryBackupJobRepository.layer >>> BackupJobServiceImpl.layer

  private val valid = CreateBackupJobRequest(
    name = "questify-postgres-daily",
    sourceType = SourceType.PostgreSQL,
    source = "postgres://questify-db",
    targetBucket = "squirrelvault-backups",
    targetPrefix = "questify/postgres",
    schedule = "0 2 * * *",
    retentionDays = 14,
    critical = true,
    expectedFrequencyHours = 24,
    enabled = true
  )

  def spec = suite("BackupJobService")(
    test("create returns job with generated id and createdAt") {
      for job <- BackupJobService.create(valid)
      yield assertTrue(job.id.nonEmpty, job.createdAt != null, job.name == "questify-postgres-daily")
    }.provide(serviceLayer),
    test("create fails with errors for invalid request") {
      val invalid = valid.copy(name = "", retentionDays = 0)
      for result <- BackupJobService.create(invalid).either
      yield assertTrue(
        result.isLeft,
        result.left.toOption.exists(_.errors.contains("name must not be empty")),
        result.left.toOption.exists(_.errors.contains("retentionDays must be greater than 0"))
      )
    }.provide(serviceLayer),
    test("list returns all created jobs") {
      for
        _ <- BackupJobService.create(valid)
        _ <- BackupJobService.create(valid.copy(name = "second-backup"))
        jobs <- BackupJobService.list(None)
      yield assertTrue(jobs.length == 2)
    }.provide(serviceLayer),
    test("list filters by enabled=true") {
      for
        job1 <- BackupJobService.create(valid)
        _ <- BackupJobService.create(valid.copy(name = "disabled-backup", enabled = false))
        jobs <- BackupJobService.list(Some(true))
      yield assertTrue(jobs.length == 1, jobs.head.id == job1.id)
    }.provide(serviceLayer),
    test("list filters by enabled=false") {
      for
        _ <- BackupJobService.create(valid)
        _ <- BackupJobService.create(valid.copy(name = "disabled-backup", enabled = false))
        jobs <- BackupJobService.list(Some(false))
      yield assertTrue(jobs.length == 1, jobs.head.name == "disabled-backup")
    }.provide(serviceLayer),
    test("findById returns the job when it exists") {
      for
        created <- BackupJobService.create(valid)
        found <- BackupJobService.findById(created.id)
      yield assertTrue(found.contains(created))
    }.provide(serviceLayer),
    test("findById returns None for unknown id") {
      for found <- BackupJobService.findById("unknown-id")
      yield assertTrue(found.isEmpty)
    }.provide(serviceLayer),
    test("disable sets enabled to false") {
      for
        created <- BackupJobService.create(valid)
        disabled <- BackupJobService.disable(created.id)
      yield assertTrue(disabled.exists(!_.enabled), disabled.exists(_.id == created.id))
    }.provide(serviceLayer),
    test("disable returns None for unknown id") {
      for result <- BackupJobService.disable("unknown-id")
      yield assertTrue(result.isEmpty)
    }.provide(serviceLayer)
  )
