package squirrelvault.api

import squirrelvault.api.dto.{BackupHistoryEntry, ValidationErrorResponse}
import squirrelvault.domain.{BackupRun, RunStatus}
import squirrelvault.repository.{BackupJobRepository, BackupRunRepository}
import squirrelvault.service.BackupRunDispatcher
import zio.*
import zio.http.*
import zio.json.*
import java.util.UUID
import zio.telemetry.opentelemetry.tracing.Tracing

object BackupRunRoutes:

  private def jsonResponse(body: String, status: Status = Status.Ok): Response =
    Response(
      status = status,
      headers = Headers(Header.ContentType(MediaType.application.json)),
      body = Body.fromString(body)
    )

  private def internalError(err: Throwable): Response =
    jsonResponse(ValidationErrorResponse(List(err.getMessage)).toJson, Status.InternalServerError)

  private def notFound: Response =
    jsonResponse(ValidationErrorResponse(List("not found")).toJson, Status.NotFound)

  private final case class RunStarted(runId: String, status: RunStatus)
  private object RunStarted:
    given JsonCodec[RunStarted] = DeriveJsonCodec.gen[RunStarted]

  val routes: Routes[BackupJobRepository & BackupRunRepository & BackupRunDispatcher & Tracing, Nothing] = Routes(
    Method.POST / "backup-jobs" / string("id") / "run" -> handler { (id: String, _: Request) =>
      ZIO.serviceWithZIO[Tracing] { tracing =>
        tracing.span(s"HTTP POST /backup-jobs/$id/run") {
          (for
            runId <- ZIO.succeed(UUID.randomUUID().toString)
            pending = BackupRun(runId, id, RunStatus.Pending, None, None, None, None, None, None)
            _ <- ZIO.serviceWithZIO[BackupRunRepository](_.create(pending))
            _ <- ZIO.serviceWithZIO[BackupRunDispatcher](_.enqueue(id, runId))
          yield jsonResponse(RunStarted(runId, RunStatus.Pending).toJson, Status.Accepted))
            .mapError(internalError)
            .merge
        }
      }
    },
    Method.GET / "backup-runs" / string("id") -> handler { (id: String, _: Request) =>
      ZIO.serviceWithZIO[BackupRunRepository](
        _.findById(id)
          .map {
            case Some(run) => jsonResponse(run.toJson)
            case None      => notFound
          }
          .mapError(internalError)
          .merge
      )
    },
    Method.GET / "backup-jobs" / string("id") / "runs" -> handler { (id: String, _: Request) =>
      ZIO.serviceWithZIO[BackupRunRepository](
        _.listByJobId(id)
          .map(runs => jsonResponse(runs.toJson))
          .mapError(internalError)
          .merge
      )
    },
    Method.GET / "backup-history" -> handler { (_: Request) =>
      ZIO.serviceWithZIO[Tracing] { tracing =>
        tracing.span("HTTP GET /backup-history") {
          (for
            jobs <- ZIO.serviceWithZIO[BackupJobRepository](_.findAll(None))
            runs <- ZIO.serviceWithZIO[BackupRunRepository](_.listAll())
            jobById = jobs.map(job => job.id -> job).toMap
            history = runs.flatMap(run => jobById.get(run.jobId).map(job => BackupHistoryEntry(job, run)))
          yield jsonResponse(history.toJson))
            .mapError(internalError)
            .merge
        }
      }
    }
  )
