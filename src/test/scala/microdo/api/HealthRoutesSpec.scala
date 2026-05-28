package microdo.api

import zio.*
import zio.test.*
import zio.http.*

object HealthRoutesSpec extends ZIOSpecDefault:

  def spec = suite("HealthRoutes")(
    test("GET /health retourne 200") {
      for
        response <- HealthRoutes.routes.runZIO(Request.get(URL(Path.decode("/health"))))
      yield assertTrue(response.status == Status.Ok)
    },
    test("GET /health retourne le corps OK") {
      for
        response <- HealthRoutes.routes.runZIO(Request.get(URL(Path.decode("/health"))))
        body     <- response.body.asString
      yield assertTrue(body == "OK")
    },
    test("GET /inconnu retourne 404") {
      for
        response <- HealthRoutes.routes.runZIO(Request.get(URL(Path.decode("/inconnu"))))
      yield assertTrue(response.status == Status.NotFound)
    },
  )
