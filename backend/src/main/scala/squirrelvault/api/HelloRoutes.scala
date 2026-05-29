package squirrelvault.api

import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import sttp.tapir.ztapir.*
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zio.*
import zio.http.{Routes, Response}

object HelloRoutes:

  val helloServerEndpoint: ZServerEndpoint[Any, ZioStreams & WebSockets] =
    Endpoints.helloEndpoint.zServerLogic(_ => ZIO.succeed("Hello World!"))

  val routes: Routes[Any, Response] =
    ZioHttpInterpreter().toHttp(helloServerEndpoint)
