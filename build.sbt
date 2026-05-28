val scala3Version = "3.8.3"
val zioVersion    = "2.1.16"
val tapirVersion  = "1.11.7"
val zioHttpVersion = "3.0.1"
val zioJsonVersion = "0.7.3"

lazy val root = project
  .in(file("."))
  .settings(
    name         := "microdo",
    version      := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "dev.zio"                     %% "zio"                   % zioVersion,
      "dev.zio"                     %% "zio-http"              % zioHttpVersion,
      "dev.zio"                     %% "zio-json"              % zioJsonVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-zio"             % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-zio"        % tapirVersion,
      "dev.zio"                     %% "zio-test"              % zioVersion % Test,
      "dev.zio"                     %% "zio-test-sbt"          % zioVersion % Test,
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  )
