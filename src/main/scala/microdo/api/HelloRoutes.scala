package microdo.api

import sttp.tapir.ztapir.*
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zio.*
import zio.http.{Routes, Response}

object HelloRoutes:

  val routes: Routes[Any, Response] =
    ZioHttpInterpreter().toHttp(
      Endpoints.hello.zServerLogic(_ => ZIO.succeed("Hello World!"))
    )
