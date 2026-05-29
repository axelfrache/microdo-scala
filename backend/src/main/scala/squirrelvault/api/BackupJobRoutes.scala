package squirrelvault.api

import squirrelvault.api.dto.{CreateBackupJobRequest, ValidationErrorResponse}
import squirrelvault.domain.BackupJob
import squirrelvault.service.BackupJobService
import zio.*
import zio.http.*
import zio.json.*

object BackupJobRoutes:

  private def jsonResponse(body: String, status: Status = Status.Ok): Response =
    Response(
      status = status,
      headers = Headers(Header.ContentType(MediaType.application.json)),
      body = Body.fromString(body)
    )

  private def notFound: Response =
    jsonResponse(ValidationErrorResponse(List("not found")).toJson, Status.NotFound)

  private def invalidJson(detail: String): Response =
    jsonResponse(ValidationErrorResponse(List(s"Invalid JSON: $detail")).toJson, Status.BadRequest)

  val routes: Routes[BackupJobService, Nothing] = Routes(
    Method.POST / "backup-jobs" -> handler { (req: Request) =>
      (for
        bodyStr <- req.body.asString.orElseFail(Response.internalServerError)
        createReq <- ZIO
          .fromEither(bodyStr.fromJson[CreateBackupJobRequest])
          .mapError(invalidJson)
        result <- BackupJobService
          .create(createReq)
          .mapError(err => jsonResponse(err.toJson, Status.BadRequest))
      yield jsonResponse(result.toJson, Status.Created)).merge
    },
    Method.GET / "backup-jobs" -> handler { (req: Request) =>
      val enabledFilter = req.url.queryParams.getAll("enabled").headOption.collect {
        case "true"  => true
        case "false" => false
      }
      BackupJobService.list(enabledFilter).map(jobs => jsonResponse(jobs.toJson))
    },
    Method.GET / "backup-jobs" / string("id") -> handler { (id: String, _: Request) =>
      BackupJobService.findById(id).map {
        case Some(job) => jsonResponse(job.toJson)
        case None      => notFound
      }
    },
    Method.PATCH / "backup-jobs" / string("id") / "disable" -> handler { (id: String, _: Request) =>
      BackupJobService.disable(id).map {
        case Some(job) => jsonResponse(job.toJson)
        case None      => notFound
      }
    }
  )
