package squirrelvault.api.dto

import squirrelvault.domain.{BackupJob, BackupRun}
import zio.json.*

final case class BackupHistoryEntry(
    job: BackupJob,
    run: BackupRun
)

object BackupHistoryEntry:
  given JsonCodec[BackupHistoryEntry] = DeriveJsonCodec.gen[BackupHistoryEntry]
