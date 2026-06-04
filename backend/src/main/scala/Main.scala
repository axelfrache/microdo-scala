import squirrelvault.api.{HealthRoutes, HelloRoutes, PersonRoutes}
import squirrelvault.api.BackupJobRoutes
import squirrelvault.api.BackupRunRoutes
import squirrelvault.repository.PostgresBackupRunRepository
import squirrelvault.service.BackupExecutor
import squirrelvault.service.InMemoryBackupRunDispatcher
import squirrelvault.service.BackupStorageConfig
import squirrelvault.repository.PostgresBackupJobRepository
import squirrelvault.service.BackupJobServiceImpl
import squirrelvault.telemetry.Telemetry
import zio.*
import zio.http.{Response, Server}
import zio.jdbc.{ZConnectionPool, ZConnectionPoolConfig}

object Main extends ZIOAppDefault:

  override def run: Task[Nothing] =
    for
      dbHost     <- ZIO.succeed(sys.env.getOrElse("DB_HOST", "localhost"))
      dbPort     <- ZIO.attempt(sys.env.getOrElse("DB_PORT", "5432").toInt)
                      .mapError(e => new RuntimeException(s"Invalid DB_PORT: ${e.getMessage}"))
      dbName     <- ZIO.succeed(sys.env.getOrElse("DB_NAME", "squirrelvault"))
      dbUser     <- ZIO.succeed(sys.env.getOrElse("DB_USER", "squirrelvault"))
      dbPassword <- ZIO.succeed(sys.env.getOrElse("DB_PASSWORD", "squirrelvault"))
      _          <- ZIO.logInfo("Starting squirrelvault on port 8080")
      _          <- ZIO.logInfo(s"Connecting to PostgreSQL at $dbHost:$dbPort/$dbName")
      result     <- Server
        .serve(
          ((HealthRoutes.routes ++ HelloRoutes.routes ++ PersonRoutes.routes)
            .handleError(_ => Response.internalServerError) ++
            BackupRunRoutes.routes ++ BackupJobRoutes.routes)
        )
        .provide(
          Server.defaultWithPort(8080),
          Telemetry.layer,
          Telemetry.loggingLayer,
          BackupJobServiceImpl.layer,
          PostgresBackupJobRepository.layer,
          PostgresBackupRunRepository.layer,
          BackupStorageConfig.layer,
          BackupExecutor.layer,
          InMemoryBackupRunDispatcher.layer,
          ZConnectionPool.postgres(dbHost, dbPort, dbName, Map("user" -> dbUser, "password" -> dbPassword)),
          ZLayer.succeed(ZConnectionPoolConfig.default)
        )
        .tapError(e => ZIO.logError(s"Server error: ${e.getMessage}"))
    yield result
