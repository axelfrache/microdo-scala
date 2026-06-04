package squirrelvault.service

import squirrelvault.domain.{BackupRun, RunStatus}
import squirrelvault.repository.{BackupJobRepository, BackupRunRepository}
import zio.*
import java.time.{Instant, ZoneOffset, ZonedDateTime}
import java.util.UUID

object BackupScheduler:

  def startBackground: RIO[BackupJobRepository & BackupRunRepository & BackupRunDispatcher, Unit] =
    for
      jobRepo <- ZIO.service[BackupJobRepository]
      runRepo <- ZIO.service[BackupRunRepository]
      dispatcher <- ZIO.service[BackupRunDispatcher]
      _ <- ZIO.logInfo("BackupScheduler started")
      _ <- tick(jobRepo, runRepo, dispatcher)
        .catchAll(err => ZIO.logError(s"Scheduler tick failed: ${err.getMessage}"))
        .repeat(Schedule.fixed(1.minute))
        .forkDaemon
    yield ()

  private def tick(
      jobRepo: BackupJobRepository,
      runRepo: BackupRunRepository,
      dispatcher: BackupRunDispatcher
  ): Task[Unit] =
    for
      now <- Clock.instant
      jobs <- jobRepo.findAll(Some(true))
      _ <- ZIO.foreachDiscard(jobs) { job =>
        ZIO.when(cronMatches(job.schedule, now)) {
          val runId = UUID.randomUUID().toString
          for
            _ <- runRepo.create(BackupRun(runId, job.id, RunStatus.Pending, None, None, None, None, None, None))
            _ <- dispatcher.enqueue(job.id, runId)
            _ <- ZIO.logInfo(s"Scheduler triggered run $runId for job ${job.name}")
          yield ()
        }
      }
    yield ()

  private def cronMatches(schedule: String, now: Instant): Boolean =
    val zdt = ZonedDateTime.ofInstant(now, ZoneOffset.UTC)
    val fields = schedule.trim.split("\\s+")
    fields.length == 5 &&
    matchField(fields(0), zdt.getMinute) &&
    matchField(fields(1), zdt.getHour) &&
    matchField(fields(2), zdt.getDayOfMonth) &&
    matchField(fields(3), zdt.getMonthValue) &&
    matchField(fields(4), zdt.getDayOfWeek.getValue % 7)

  private def matchField(field: String, value: Int): Boolean =
    field.split(",").exists(matchPart(_, value))

  private def matchPart(part: String, value: Int): Boolean =
    val slashIdx = part.indexOf('/')
    if slashIdx >= 0 then
      val step = part.substring(slashIdx + 1).toInt
      val baseStr = part.substring(0, slashIdx)
      val start = if baseStr == "*" then 0 else baseStr.toInt
      value >= start && (value - start) % step == 0
    else if part.contains("-") then
      val dashIdx = part.indexOf('-')
      value >= part.substring(0, dashIdx).toInt && value <= part.substring(dashIdx + 1).toInt
    else if part == "*" then true
    else part.toIntOption.contains(value)
