package squirrelvault.validation

import squirrelvault.api.dto.CreateBackupJobRequest
import squirrelvault.domain.SourceType
import zio.test.*

object BackupJobValidatorSpec extends ZIOSpecDefault:

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

  def spec = suite("BackupJobValidator")(
    test("valid request passes validation") {
      assertTrue(BackupJobValidator.validate(valid).isEmpty)
    },
    test("empty name produces error") {
      val errors = BackupJobValidator.validate(valid.copy(name = ""))
      assertTrue(errors.contains("name must not be empty"))
    },
    test("blank name produces error") {
      val errors = BackupJobValidator.validate(valid.copy(name = "   "))
      assertTrue(errors.contains("name must not be empty"))
    },
    test("empty source produces error") {
      val errors = BackupJobValidator.validate(valid.copy(source = ""))
      assertTrue(errors.contains("source must not be empty"))
    },
    test("empty targetBucket produces error") {
      val errors = BackupJobValidator.validate(valid.copy(targetBucket = ""))
      assertTrue(errors.contains("targetBucket must not be empty"))
    },
    test("empty targetPrefix produces error") {
      val errors = BackupJobValidator.validate(valid.copy(targetPrefix = ""))
      assertTrue(errors.contains("targetPrefix must not be empty"))
    },
    test("empty schedule produces error") {
      val errors = BackupJobValidator.validate(valid.copy(schedule = ""))
      assertTrue(errors.contains("schedule must not be empty"))
    },
    test("retentionDays = 0 produces error") {
      val errors = BackupJobValidator.validate(valid.copy(retentionDays = 0))
      assertTrue(errors.contains("retentionDays must be greater than 0"))
    },
    test("retentionDays < 0 produces error") {
      val errors = BackupJobValidator.validate(valid.copy(retentionDays = -5))
      assertTrue(errors.contains("retentionDays must be greater than 0"))
    },
    test("expectedFrequencyHours = 0 produces error") {
      val errors = BackupJobValidator.validate(valid.copy(expectedFrequencyHours = 0))
      assertTrue(errors.contains("expectedFrequencyHours must be greater than 0"))
    },
    test("multiple invalid fields produce all errors") {
      val req = valid.copy(name = "", retentionDays = -1, expectedFrequencyHours = 0)
      val errors = BackupJobValidator.validate(req)
      assertTrue(
        errors.contains("name must not be empty"),
        errors.contains("retentionDays must be greater than 0"),
        errors.contains("expectedFrequencyHours must be greater than 0"),
        errors.length == 3
      )
    }
  )
