import microdo.api.{HealthRoutes, HelloRoutes, PersonRoutes}
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
            .handleError(_ => Response.internalServerError)
        )
        .provide(Server.defaultWithPort(port))
        .tapError(e => ZIO.logError(s"Server error: ${e.getMessage}"))
    yield result
