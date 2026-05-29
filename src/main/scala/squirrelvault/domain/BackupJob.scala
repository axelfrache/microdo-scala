package squirrelvault.domain

import zio.json.*
import java.time.Instant
import scala.util.Try

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
  given JsonDecoder[Instant] = JsonDecoder.string.mapOrFail(s => Try(Instant.parse(s)).toEither.left.map(_.getMessage))
  given JsonEncoder[Instant] = JsonEncoder.string.contramap(_.toString)
  given JsonCodec[Instant]   = JsonCodec(JsonEncoder[Instant], JsonDecoder[Instant])

  given JsonCodec[BackupJob] = DeriveJsonCodec.gen[BackupJob]
