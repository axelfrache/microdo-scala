package squirrelvault.domain

import zio.json.*
import java.time.Instant
import scala.util.Try

object CommonCodecs:
  given JsonDecoder[Instant] = JsonDecoder.string.mapOrFail(s =>
    Try(Instant.parse(s)).toEither.left.map(_.getMessage)
  )
  given JsonEncoder[Instant] = JsonEncoder.string.contramap(_.toString)
  given JsonCodec[Instant]   = JsonCodec(JsonEncoder[Instant], JsonDecoder[Instant])
