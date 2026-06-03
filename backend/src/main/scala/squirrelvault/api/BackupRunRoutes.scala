package squirrelvault.api

import squirrelvault.domain.BackupRun
import squirrelvault.repository.BackupRunRepository
import squirrelvault.service.BackupRunDispatcher
import zio.*
import zio.http.*
import zio.json.*
import java.util.UUID
import zio.telemetry.opentelemetry.tracing.Tracing

object BackupRunRoutes:

  private def jsonResponse(body: String, status: Status = Status.Ok): Response =
    Response(status = status, headers = Headers(Header.ContentType(MediaType.application.json)), body = Body.fromString(body))

  val routes: Routes[BackupRunRepository & BackupRunDispatcher & Tracing, Nothing] = Routes(
    Method.POST / "backup-jobs" / string("id") / "run" -> handler { (id: String, _: Request) =>
      ZIO.environment[BackupRunRepository & BackupRunDispatcher & Tracing].flatMap { env =>
        val repo = env.get[BackupRunRepository]
        val dispatcher = env.get[BackupRunDispatcher]
        val tracing = env.get[Tracing]
        tracing.span(s"HTTP POST /backup-jobs/$id/run") {
          val runId = UUID.randomUUID().toString
          val pending = BackupRun(runId, id, "PENDING", None, None, None, None, None, None)
          (for
            _ <- repo.create(pending)
            _ <- dispatcher.enqueue(id, runId)
          yield jsonResponse(s"{\"runId\":\"$runId\",\"status\":\"PENDING\"}", Status.Accepted))
            .mapError(err => jsonResponse(s"{\"errors\":[\"${err.getMessage}\"]}", Status.InternalServerError))
            .merge
        }
      }
    },

    Method.GET / "backup-runs" / string("id") -> handler { (id: String, _: Request) =>
      ZIO.environment[BackupRunRepository & BackupRunDispatcher & Tracing].flatMap { env =>
        val repo = env.get[BackupRunRepository]
        repo.findById(id).map {
          case Some(run) => jsonResponse(run.toJson)
          case None      => jsonResponse("{\"errors\":[\"not found\"]}", Status.NotFound)
        }.mapError(err => jsonResponse(s"{\"errors\":[\"${err.getMessage}\"]}", Status.InternalServerError)).merge
      }
    },

    Method.GET / "backup-jobs" / string("id") / "runs" -> handler { (id: String, _: Request) =>
      ZIO.environment[BackupRunRepository & BackupRunDispatcher & Tracing].flatMap { env =>
        val repo = env.get[BackupRunRepository]
        repo.listByJobId(id).map(runs => jsonResponse(runs.toJson)).mapError(err => jsonResponse(s"{\"errors\":[\"${err.getMessage}\"]}", Status.InternalServerError)).merge
      }
    }
  )
