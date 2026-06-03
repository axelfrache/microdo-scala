package squirrelvault.api

import squirrelvault.repository.InMemoryBackupRunRepository
import squirrelvault.repository.InMemoryBackupJobRepository
import squirrelvault.service.SimpleBackupExecutor
import squirrelvault.service.BackupExecutor
import zio.*
import squirrelvault.repository.BackupRunRepository
import squirrelvault.service.BackupRunDispatcher
import squirrelvault.service.InMemoryBackupRunDispatcher
import zio.telemetry.opentelemetry.tracing.Tracing
import squirrelvault.telemetry.Telemetry
import zio.http.*
import zio.json.*
import zio.test.*

final case class StartRunResponse(runId: String, status: String)

object StartRunResponse:
  given JsonCodec[StartRunResponse] = DeriveJsonCodec.gen[StartRunResponse]

object BackupRunRoutesSpec extends ZIOSpecDefault:

  private val runLayer = ZLayer.make[BackupRunRepository & BackupExecutor & BackupRunDispatcher & Tracing](
    InMemoryBackupRunRepository.layer,
    SimpleBackupExecutor.layer,
    InMemoryBackupRunDispatcher.layer,
    Telemetry.layer
  )

  def spec = suite("BackupRunRoutes")(
    test("POST start and GET status") {
      ZIO.scoped {
        runLayer.build.flatMap { env =>
        val request = Request.post("/backup-jobs/myjob/run", Body.empty)

        for
          response <- BackupRunRoutes.routes.runZIO(request).provideEnvironment(env)
          body <- response.body.asString
          startRun <- ZIO.fromEither(body.fromJson[StartRunResponse].left.map(error => new RuntimeException(error)))
          _ <- TestClock.adjust(3.seconds)
          statusResponse <- BackupRunRoutes.routes.runZIO(Request.get(URL(Path.decode(s"/backup-runs/${startRun.runId}")))).provideEnvironment(env)
          statusBody <- statusResponse.body.asString
          run <- ZIO.fromEither(statusBody.fromJson[squirrelvault.domain.BackupRun].left.map(error => new RuntimeException(error)))
        yield assertTrue(response.status == Status.Accepted, startRun.status == "PENDING", statusResponse.status == Status.Ok, run.status == "SUCCESS")
        }
      }
    }
  )
