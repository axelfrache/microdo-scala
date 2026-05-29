package squirrelvault.api

import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.generic.auto.*

object Endpoints:

  val healthEndpoint: PublicEndpoint[Unit, Unit, String, Any] =
    endpoint.get
      .in("health")
      .out(stringBody)
      .description("Service health check")

  val helloEndpoint: PublicEndpoint[Unit, Unit, String, Any] =
    endpoint.get
      .in("hello")
      .out(stringBody)
      .description("Hello world")

  val peopleEndpoint: PublicEndpoint[Unit, Unit, List[Person], Any] =
    endpoint.get
      .in("people")
      .out(jsonBody[List[Person]])
      .description("People list")
