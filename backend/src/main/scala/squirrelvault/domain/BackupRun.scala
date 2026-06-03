package squirrelvault.domain

import zio.json.*
import java.time.Instant

final case class BackupRun(
    id: String,
    jobId: String,
    status: String,
    startedAt: Option[Instant],
    finishedAt: Option[Instant],
    sizeMb: Option[Long],
    location: Option[String],
    error: Option[String]
)

object BackupRun:
  given JsonCodec[BackupRun] = DeriveJsonCodec.gen[BackupRun]
