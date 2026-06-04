package squirrelvault.domain

import zio.json.*

enum RunStatus:
  case Pending, Running, Success, Failed

object RunStatus:
  def fromDb(s: String): RunStatus =
    values
      .find(_.toString.equalsIgnoreCase(s))
      .getOrElse(throw new IllegalStateException(s"Unknown RunStatus: $s"))

  def toDb(status: RunStatus): String = status.toString.toUpperCase

  given JsonDecoder[RunStatus] = JsonDecoder.string.mapOrFail { s =>
    values
      .find(_.toString.equalsIgnoreCase(s))
      .toRight(s"Invalid RunStatus: '$s'. Valid values: ${values.map(toDb).mkString(", ")}")
  }
  given JsonEncoder[RunStatus] = JsonEncoder.string.contramap(toDb)
  given JsonCodec[RunStatus] = JsonCodec(JsonEncoder[RunStatus], JsonDecoder[RunStatus])
