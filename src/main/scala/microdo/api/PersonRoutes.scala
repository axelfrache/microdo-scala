package microdo.api

import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.ztapir.*
import zio.*
import zio.http.{Response, Routes}

object PersonRoutes:

  private val people = List(
    Person(id = 1, firstName = "Axel", lastName = "Frache", email = "axel.frache@etu.umontpellier.fr")
  )

  val peopleServerEndpoint: ZServerEndpoint[Any, ZioStreams & WebSockets] =
    Endpoints.peopleEndpoint.zServerLogic(_ => ZIO.succeed(people))

  val routes: Routes[Any, Response] =
    ZioHttpInterpreter().toHttp(peopleServerEndpoint)
