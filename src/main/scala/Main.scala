import microdo.api.HealthRoutes
import zio.*
import zio.http.{Response, Server}

object Main extends ZIOAppDefault:

  override def run: Task[Nothing] =
    Server
      .serve(HealthRoutes.routes.handleError(_ => Response.internalServerError))
      .provide(Server.defaultWithPort(8080))
      .tapError(e => ZIO.logError(s"Erreur serveur : ${e.getMessage}"))
