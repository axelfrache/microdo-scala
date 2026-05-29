package squirrelvault.validation

import squirrelvault.api.dto.CreateBackupJobRequest

object BackupJobValidator:
  def validate(req: CreateBackupJobRequest): List[String] =
    List(
      Option.when(req.name.isBlank)("name must not be empty"),
      Option.when(req.source.isBlank)("source must not be empty"),
      Option.when(req.targetBucket.isBlank)("targetBucket must not be empty"),
      Option.when(req.targetPrefix.isBlank)("targetPrefix must not be empty"),
      Option.when(req.schedule.isBlank)("schedule must not be empty"),
      Option.when(req.retentionDays <= 0)("retentionDays must be greater than 0"),
      Option.when(req.expectedFrequencyHours <= 0)("expectedFrequencyHours must be greater than 0")
    ).flatten
