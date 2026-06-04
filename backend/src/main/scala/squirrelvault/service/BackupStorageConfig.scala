package squirrelvault.service

import zio.*

final case class BackupStorageConfig(
    endpoint: String,
    accessKey: String,
    secretKey: String
)

object BackupStorageConfig:
  val layer: ULayer[BackupStorageConfig] =
    ZLayer.succeed {
      BackupStorageConfig(
        endpoint = sys.env.getOrElse("MINIO_ENDPOINT", "http://localhost:9000"),
        accessKey = sys.env.getOrElse("MINIO_ACCESS_KEY", sys.env.getOrElse("MINIO_ROOT_USER", "minio")),
        secretKey = sys.env.getOrElse("MINIO_SECRET_KEY", sys.env.getOrElse("MINIO_ROOT_PASSWORD", "changeme"))
      )
    }
