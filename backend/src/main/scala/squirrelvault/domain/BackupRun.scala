package squirrelvault.domain

import zio.json.*
import java.time.Instant
import CommonCodecs.given

final case class BackupRun(
    id: String,
    jobId: String,
    status: RunStatus,
    startedAt: Option[Instant],
    finishedAt: Option[Instant],
    duration: Option[Long],
    sizeMb: Option[Long],
    location: Option[String],
    error: Option[String]
)

object BackupRun:
  given JsonCodec[BackupRun] = DeriveJsonCodec.gen[BackupRun]
