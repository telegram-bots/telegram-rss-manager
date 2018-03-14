import sbt.Keys.libraryDependencies

name := Build.namePrefix + "root"

lazy val commonSettings = Seq(
  organization := "telegram-bots",
  scalaVersion := Build.scalaVersion,

  libraryDependencies ++= Seq(
    // Config
    "com.typesafe" % "config"                                     % "1.3.3" % "provided",

    // Akka
    "com.typesafe.akka" %% "akka-actor"                           % Dependencies.akkaVersion % "provided",
    "com.typesafe.akka" %% "akka-stream"                          % Dependencies.akkaVersion % "provided",
    "com.typesafe.akka" %% "akka-http"                            % Dependencies.akkaHttpVersion % "provided",

    // DB
    "com.typesafe.slick" %% "slick"                               % Dependencies.slickVersion % "provided",

    // Tests
    "org.scalatest" %% "scalatest"                                % "3.0.5" % Test
  )
)

lazy val core = project
  .settings(commonSettings)

lazy val updater = project
  .settings(commonSettings)
  .dependsOn(core)
  .enablePlugins(DockerPlugin)

lazy val web = project
  .settings(commonSettings)
  .dependsOn(core)
  .enablePlugins(DockerPlugin)

lazy val root = (project in file("."))
  .settings(commonSettings)
  .aggregate(core, updater, web)