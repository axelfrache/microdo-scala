package squirrelvault.api.dto

import zio.json.*

final case class ValidationErrorResponse(errors: List[String])

object ValidationErrorResponse:
  given JsonCodec[ValidationErrorResponse] = DeriveJsonCodec.gen[ValidationErrorResponse]
