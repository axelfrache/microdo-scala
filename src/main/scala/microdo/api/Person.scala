package microdo.api

import zio.json.*

final case class Person(id: Int, firstName: String, lastName: String, email: String)

object Person:
  given JsonCodec[Person] = DeriveJsonCodec.gen[Person]
