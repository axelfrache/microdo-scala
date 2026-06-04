package squirrelvault.repository

import squirrelvault.domain.{BackupRun, RunStatus}
import zio.*
import zio.jdbc.*
import java.sql.ResultSet

final class PostgresBackupRunRepository(pool: ZConnectionPool) extends BackupRunRepository:

  private def readLong(column: String, rs: ResultSet): Option[Long] =
    Option(rs.getObject(column)).map(_.asInstanceOf[Number].longValue)

  private given JdbcDecoder[BackupRun] = new JdbcDecoder[BackupRun]:
    def unsafeDecode(columnIndex: Int, rs: ResultSet): (Int, BackupRun) =
      (
        columnIndex,
        BackupRun(
          id = rs.getString("id"),
          jobId = rs.getString("job_id"),
          status = RunStatus.fromDb(rs.getString("status")),
          startedAt = Option(rs.getTimestamp("started_at")).map(_.toInstant),
          finishedAt = Option(rs.getTimestamp("finished_at")).map(_.toInstant),
          duration = readLong("duration", rs),
          sizeMb = readLong("size_mb", rs),
          location = Option(rs.getString("location")).filterNot(_.isBlank),
          error = Option(rs.getString("error")).filterNot(_.isBlank)
        )
      )

  private val selectAll =
    sql"SELECT id, job_id, status, started_at, finished_at, duration, size_mb, location, error FROM backup_runs"

  def initSchema: Task[Unit] =
    transaction {
      sql"""
        CREATE TABLE IF NOT EXISTS backup_runs (
          id          VARCHAR(36) PRIMARY KEY,
          job_id      VARCHAR(36) NOT NULL,
          status      VARCHAR(20) NOT NULL,
          started_at  TIMESTAMPTZ,
          finished_at TIMESTAMPTZ,
          duration    BIGINT,
          size_mb     BIGINT,
          location    TEXT,
          error       TEXT,
          created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
          updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
        )
      """.execute
    }.unit.provide(ZLayer.succeed(pool))

  def create(run: BackupRun): Task[BackupRun] =
    transaction {
      sql"""
        INSERT INTO backup_runs
          (id, job_id, status, started_at, finished_at, duration, size_mb, location, error)
        VALUES
          (${run.id}, ${run.jobId}, ${RunStatus.toDb(run.status)},
           ${run.startedAt.map(java.sql.Timestamp.from)},
           ${run.finishedAt.map(java.sql.Timestamp.from)},
           ${run.duration},
           ${run.sizeMb},
           ${run.location},
           ${run.error})
      """.execute
    }.as(run).provide(ZLayer.succeed(pool))

  def update(run: BackupRun): Task[BackupRun] =
    transaction {
      sql"""
        UPDATE backup_runs
        SET job_id = ${run.jobId},
            status = ${RunStatus.toDb(run.status)},
            started_at = ${run.startedAt.map(java.sql.Timestamp.from)},
            finished_at = ${run.finishedAt.map(java.sql.Timestamp.from)},
            duration = ${run.duration},
            size_mb = ${run.sizeMb},
            location = ${run.location},
            error = ${run.error},
            updated_at = NOW()
        WHERE id = ${run.id}
      """.execute
    }.as(run).provide(ZLayer.succeed(pool))

  def findById(id: String): Task[Option[BackupRun]] =
    transaction {
      (selectAll ++ sql" WHERE id = $id").query[BackupRun].selectOne
    }.provide(ZLayer.succeed(pool))

  def listByJobId(jobId: String): Task[List[BackupRun]] =
    transaction {
      (selectAll ++ sql" WHERE job_id = $jobId ORDER BY created_at DESC").query[BackupRun].selectAll
    }.map(_.toList).provide(ZLayer.succeed(pool))

  def listAll(): Task[List[BackupRun]] =
    transaction {
      (selectAll ++ sql" ORDER BY created_at DESC").query[BackupRun].selectAll
    }.map(_.toList).provide(ZLayer.succeed(pool))

object PostgresBackupRunRepository:
  val layer: RLayer[ZConnectionPool, BackupRunRepository] =
    ZLayer {
      for
        pool <- ZIO.service[ZConnectionPool]
        repo = new PostgresBackupRunRepository(pool)
        _ <- repo.initSchema
      yield repo
    }
