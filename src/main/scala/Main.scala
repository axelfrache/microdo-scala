import microdo.api.{HealthRoutes, HelloRoutes, PersonRoutes}
import squirrelvault.api.BackupJobRoutes
import squirrelvault.repository.InMemoryBackupJobRepository
import squirrelvault.service.BackupJobServiceImpl
import zio.*
import zio.http.{Response, Server}

object Main extends ZIOAppDefault:

  private val port = 8080

  override def run: Task[Nothing] =
    for
      _ <- ZIO.logInfo(s"Starting microdo on port $port")
      result <- Server
        .serve(
          (HealthRoutes.routes ++ HelloRoutes.routes ++ PersonRoutes.routes)
            .handleError(_ => Response.internalServerError) ++
            BackupJobRoutes.routes
        )
        .provide(
          Server.defaultWithPort(port),
          BackupJobServiceImpl.layer,
          InMemoryBackupJobRepository.layer
        )
        .tapError(e => ZIO.logError(s"Server error: ${e.getMessage}"))
    yield result
