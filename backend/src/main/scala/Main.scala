import squirrelvault.api.{HealthRoutes, HelloRoutes, PersonRoutes}
import squirrelvault.api.BackupJobRoutes
import squirrelvault.repository.PostgresBackupJobRepository
import squirrelvault.service.BackupJobServiceImpl
import squirrelvault.telemetry.Telemetry
import zio.*
import zio.http.{Response, Server}
import zio.jdbc.{ZConnectionPool, ZConnectionPoolConfig}

object Main extends ZIOAppDefault:

  private val port = 8080

  private val dbHost = sys.env.getOrElse("DB_HOST", "localhost")
  private val dbPort = sys.env.getOrElse("DB_PORT", "5432").toInt
  private val dbName = sys.env.getOrElse("DB_NAME", "squirrelvault")
  private val dbUser = sys.env.getOrElse("DB_USER", "squirrelvault")
  private val dbPassword = sys.env.getOrElse("DB_PASSWORD", "squirrelvault")

  override def run: Task[Nothing] =
    for
      _ <- ZIO.logInfo(s"Starting squirrelvault on port $port")
      _ <- ZIO.logInfo(s"Connecting to PostgreSQL at $dbHost:$dbPort/$dbName")
      result <- Server
        .serve(
          (HealthRoutes.routes ++ HelloRoutes.routes ++ PersonRoutes.routes)
            .handleError(_ => Response.internalServerError) ++
            BackupJobRoutes.routes
        )
        .provide(
          Server.defaultWithPort(port),
          Telemetry.layer,
          Telemetry.loggingLayer,
          Telemetry.sdkLayer,
          BackupJobServiceImpl.layer,
          PostgresBackupJobRepository.layer,
          ZConnectionPool.postgres(dbHost, dbPort, dbName, Map("user" -> dbUser, "password" -> dbPassword)),
          ZLayer.succeed(ZConnectionPoolConfig.default)
        )
        .tapError(e => ZIO.logError(s"Server error: ${e.getMessage}"))
    yield result
