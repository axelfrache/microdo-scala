package microdo.api

import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.generic.auto.*

object Endpoints:

  val health: PublicEndpoint[Unit, Unit, String, Any] =
    endpoint.get
      .in("health")
      .out(stringBody)
      .description("Service health check")

  val hello: PublicEndpoint[Unit, Unit, String, Any] =
    endpoint.get
      .in("hello")
      .out(stringBody)
      .description("Hello world")
