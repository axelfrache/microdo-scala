package squirrelvault.api

import squirrelvault.repository.InMemoryBackupJobRepository
import squirrelvault.service.BackupJobService
import squirrelvault.service.BackupJobServiceImpl
import squirrelvault.telemetry.Telemetry
import zio.*
import zio.http.*
import zio.json.*
import zio.test.*

object BackupJobRoutesSpec extends ZIOSpecDefault:

  private val serviceLayer: ULayer[BackupJobService] =
    InMemoryBackupJobRepository.layer >>> BackupJobServiceImpl.layer

  private val routeLayer =
    serviceLayer ++ Telemetry.layer

  private val invalidCronBody =
    """{
      |  "name": "questify-postgres-daily",
      |  "sourceType": "PostgreSQL",
      |  "source": "postgres://questify-db",
      |  "targetBucket": "squirrelvault-backups",
      |  "targetPrefix": "questify/postgres",
      |  "schedule": "0, 30 * * * *",
      |  "retentionDays": 14,
      |  "critical": true,
      |  "expectedFrequencyHours": 24,
      |  "enabled": true
      |}""".stripMargin

  def spec = suite("BackupJobRoutes")(
    test("POST /backup-jobs returns 400 for invalid cron schedule") {
      val request = Request.post(
        URL(Path.decode("/backup-jobs")),
        Body.fromString(invalidCronBody)
      )

      for
        response <- BackupJobRoutes.routes.runZIO(request).provide(routeLayer)
        body <- response.body.asString
      yield assertTrue(
        response.status == Status.BadRequest,
        body.contains("schedule must be a valid 5-field cron expression")
      )
    }
  )
