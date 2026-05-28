package microdo.api

import sttp.tapir.ztapir.*
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zio.*
import zio.http.{Routes, Response}

object HealthRoutes:

  val routes: Routes[Any, Response] =
    ZioHttpInterpreter().toHttp(
      Endpoints.health.zServerLogic(_ => ZIO.succeed("OK"))
    )
