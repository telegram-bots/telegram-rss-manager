name := Build.namePrefix + "updater"

version := "0.0.1"

enablePlugins(sbtdocker.DockerPlugin, JavaAppPackaging, AshScriptPlugin)

libraryDependencies ++= Seq(
  // Akka
  "com.typesafe.akka" %% "akka-actor"                           % Dependencies.akkaVersion,
  "com.typesafe.akka" %% "akka-stream"                          % Dependencies.akkaVersion,
  "com.typesafe.akka" %% "akka-remote"                          % Dependencies.akkaVersion,
  "com.typesafe.akka" %% "akka-http"                            % Dependencies.akkaHttpVersion,

  // Logging
  "com.typesafe.akka" %% "akka-slf4j"                           % Dependencies.akkaVersion,
  "ch.qos.logback"    %  "logback-classic"                      % Dependencies.logbackVersion,

  // DB
  "com.typesafe.slick" %% "slick"                               % Dependencies.slickVersion,
  "com.typesafe.slick" %% "slick-hikaricp"                      % Dependencies.slickVersion,
  "org.postgresql"     %  "postgresql"                          % Dependencies.postgresVersion,

  "org.jsoup" % "jsoup"                                         % "1.11.2",
  "org.json4s" %% "json4s-native"                               % "3.5.3",

  // Tests
  "com.typesafe.akka" %% "akka-testkit"                         % Dependencies.akkaVersion % Test
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