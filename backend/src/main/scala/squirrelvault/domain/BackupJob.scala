package squirrelvault.domain

import zio.json.*
import java.time.Instant
import CommonCodecs.given

final case class BackupJob(
    id: String,
    name: String,
    sourceType: SourceType,
    source: String,
    targetBucket: String,
    targetPrefix: String,
    schedule: String,
    retentionDays: Int,
    critical: Boolean,
    expectedFrequencyHours: Int,
    enabled: Boolean,
    createdAt: Instant
)

object BackupJob:
  given JsonCodec[BackupJob] = DeriveJsonCodec.gen[BackupJob]
