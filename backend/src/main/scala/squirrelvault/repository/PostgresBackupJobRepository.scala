package squirrelvault.repository

import squirrelvault.domain.{BackupJob, SourceType}
import zio.*
import zio.jdbc.*
import java.sql.ResultSet

final class PostgresBackupJobRepository(pool: ZConnectionPool) extends BackupJobRepository:

  private given JdbcDecoder[BackupJob] = new JdbcDecoder[BackupJob]:
    def unsafeDecode(columnIndex: Int, rs: ResultSet): (Int, BackupJob) =
      (
        columnIndex,
        BackupJob(
          id = rs.getString("id"),
          name = rs.getString("name"),
          sourceType = SourceType.valueOf(rs.getString("source_type")),
          source = rs.getString("source"),
          targetBucket = rs.getString("target_bucket"),
          targetPrefix = rs.getString("target_prefix"),
          schedule = rs.getString("schedule"),
          retentionDays = rs.getInt("retention_days"),
          critical = rs.getBoolean("critical"),
          expectedFrequencyHours = rs.getInt("expected_frequency_hours"),
          enabled = rs.getBoolean("enabled"),
          createdAt = rs.getTimestamp("created_at").toInstant
        )
      )

  private val selectAll =
    sql"SELECT id, name, source_type, source, target_bucket, target_prefix, schedule, retention_days, critical, expected_frequency_hours, enabled, created_at FROM backup_jobs"

  def initSchema: Task[Unit] =
    transaction {
      sql"""
        CREATE TABLE IF NOT EXISTS backup_jobs (
          id                       VARCHAR(36)  PRIMARY KEY,
          name                     VARCHAR(255) NOT NULL,
          source_type              VARCHAR(50)  NOT NULL,
          source                   VARCHAR(500) NOT NULL,
          target_bucket            VARCHAR(255) NOT NULL,
          target_prefix            VARCHAR(255) NOT NULL,
          schedule                 VARCHAR(100) NOT NULL,
          retention_days           INT          NOT NULL,
          critical                 BOOLEAN      NOT NULL,
          expected_frequency_hours INT          NOT NULL,
          enabled                  BOOLEAN      NOT NULL,
          created_at               TIMESTAMPTZ  NOT NULL
        )
      """.execute
    }.unit.provide(ZLayer.succeed(pool))

  def save(job: BackupJob): Task[BackupJob] =
    transaction {
      sql"""
        INSERT INTO backup_jobs
          (id, name, source_type, source, target_bucket, target_prefix, schedule, retention_days, critical, expected_frequency_hours, enabled, created_at)
        VALUES
          (${job.id}, ${job.name}, ${job.sourceType.toString}, ${job.source},
           ${job.targetBucket}, ${job.targetPrefix}, ${job.schedule},
           ${job.retentionDays}, ${job.critical}, ${job.expectedFrequencyHours},
           ${job.enabled}, ${java.sql.Timestamp.from(job.createdAt)})
      """.execute
    }.as(job).provide(ZLayer.succeed(pool))

  def findAll(enabled: Option[Boolean]): Task[List[BackupJob]] =
    transaction {
      val query = enabled.fold(selectAll)(e => selectAll ++ sql" WHERE enabled = $e")
      query.query[BackupJob].selectAll
    }.map(_.toList).provide(ZLayer.succeed(pool))

  def findById(id: String): Task[Option[BackupJob]] =
    transaction {
      (selectAll ++ sql" WHERE id = $id").query[BackupJob].selectOne
    }.provide(ZLayer.succeed(pool))

  def disable(id: String): Task[Option[BackupJob]] =
    transaction {
      for
        _ <- sql"UPDATE backup_jobs SET enabled = false WHERE id = $id".execute
        result <- (selectAll ++ sql" WHERE id = $id").query[BackupJob].selectOne
      yield result
    }.provide(ZLayer.succeed(pool))

  def enable(id: String): Task[Option[BackupJob]] =
    transaction {
      for
        _ <- sql"UPDATE backup_jobs SET enabled = true WHERE id = $id".execute
        result <- (selectAll ++ sql" WHERE id = $id").query[BackupJob].selectOne
      yield result
    }.provide(ZLayer.succeed(pool))

object PostgresBackupJobRepository:
  val layer: RLayer[ZConnectionPool, BackupJobRepository] =
    ZLayer {
      for
        pool <- ZIO.service[ZConnectionPool]
        repo = new PostgresBackupJobRepository(pool)
        _ <- repo.initSchema
      yield repo
    }
