package squirrelvault.domain

import zio.json.*

enum SourceType:
  case PostgreSQL, MySQL, Filesystem, KubernetesPVC, S3Bucket, ConfigFiles

object SourceType:
  given JsonDecoder[SourceType] = JsonDecoder.string.mapOrFail { s =>
    SourceType.values
      .find(_.toString == s)
      .toRight(s"Invalid SourceType: '$s'. Valid values: ${SourceType.values.map(_.toString).mkString(", ")}")
  }
  given JsonEncoder[SourceType] = JsonEncoder.string.contramap(_.toString)
  given JsonCodec[SourceType] = JsonCodec(JsonEncoder[SourceType], JsonDecoder[SourceType])
