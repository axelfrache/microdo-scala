package squirrelvault.api

import zio.*
import zio.test.*
import zio.http.*

object HelloRoutesSpec extends ZIOSpecDefault:

  def spec = suite("HelloRoutes")(
    test("GET /hello returns 200") {
      for response <- HelloRoutes.routes.runZIO(Request.get(URL(Path.decode("/hello"))))
      yield assertTrue(response.status == Status.Ok)
    },
    test("GET /hello returns Hello World!") {
      for
        response <- HelloRoutes.routes.runZIO(Request.get(URL(Path.decode("/hello"))))
        body <- response.body.asString
      yield assertTrue(body == "Hello World!")
    }
  )
