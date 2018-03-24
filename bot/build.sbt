name := Build.namePrefix + "bot"

version := "0.0.1"

enablePlugins(sbtdocker.DockerPlugin, JavaAppPackaging, AshScriptPlugin)

libraryDependencies ++= Seq(
  // Bot
  "info.mukel" %% "telegrambot4s"                               % "3.0.14",

  // Logging
  "com.typesafe.akka" %% "akka-slf4j"                           % Dependencies.akkaVersion,
  "ch.qos.logback"    %  "logback-classic"                      % Dependencies.logbackVersion,

  // DB
  "com.typesafe.slick" %% "slick"                               % Dependencies.slickVersion,
  "com.typesafe.slick" %% "slick-hikaricp"                      % Dependencies.slickVersion,
  "org.postgresql"     %  "postgresql"                          % Dependencies.postgresVersion,

  // Tests
  "com.typesafe.akka" %% "akka-testkit"                         % Dependencies.akkaVersion % Test
)

dependencyOverrides ++= Seq(
  // Akka
  "com.typesafe.akka" %% "akka-actor"                           % Dependencies.akkaVersion,
  "com.typesafe.akka" %% "akka-stream"                          % Dependencies.akkaVersion,
  "com.typesafe.akka" %% "akka-http"                            % Dependencies.akkaHttpVersion
)

dockerfile in docker := {
  val appDir: File = stage.value
  val targetDir = "/app"

  new Dockerfile {
    from("openjdk:8-jre-alpine")
    entryPoint(s"$targetDir/bin/${executableScriptName.value}")
    copy(appDir, targetDir, chown = "daemon:daemon")
  }
}

imageNames in docker := Seq(
  ImageName(
    namespace = Some(organization.value),
    repository = name.value,
    tag = Some(if (isSnapshot.value) "latest" else "v" + version.value)
  )
)