package squirrelvault.api.dto

import zio.json.*
import squirrelvault.domain.SourceType

final case class CreateBackupJobRequest(
  name: String,
  sourceType: SourceType,
  source: String,
  targetBucket: String,
  targetPrefix: String,
  schedule: String,
  retentionDays: Int,
  critical: Boolean,
  expectedFrequencyHours: Int,
  enabled: Boolean
)

object CreateBackupJobRequest:
  given JsonCodec[CreateBackupJobRequest] = DeriveJsonCodec.gen[CreateBackupJobRequest]
