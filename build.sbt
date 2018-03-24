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

    // IOC
    "com.softwaremill.macwire" %% "macros"                        % Dependencies.macWireVersion % "provided",
    "com.softwaremill.macwire" %% "macrosakka"                    % Dependencies.macWireVersion % "provided",
    "com.softwaremill.macwire" %% "util"                          % Dependencies.macWireVersion,
    "com.softwaremill.macwire" %% "proxy"                         % Dependencies.macWireVersion,

    // Tests
    "org.scalatest" %% "scalatest"                                % "3.0.5" % Test
  )
)

lazy val core = project
  .settings(commonSettings)

lazy val bot = project
  .settings(commonSettings)
  .dependsOn(core)
  .enablePlugins(DockerPlugin)

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
  .aggregate(core, bot, updater, web)