package microdo.api

import zio.*
import zio.http.*
import zio.json.*
import zio.test.*

object PersonRoutesSpec extends ZIOSpecDefault:

  def spec = suite("PersonRoutes")(
    test("GET /people returns 200") {
      for response <- PersonRoutes.routes.runZIO(Request.get(URL(Path.decode("/people"))))
      yield assertTrue(response.status == Status.Ok)
    },
    test("GET /people returns people as json") {
      for
        response <- PersonRoutes.routes.runZIO(Request.get(URL(Path.decode("/people"))))
        body <- response.body.asString
      yield assertTrue(
        body.fromJson[List[Person]] == Right(
          List(
            Person(id = 1, firstName = "Axel", lastName = "Frache", email = "axel.frache@etu.umontpellier.fr")
          )
        )
      )
    }
  )
