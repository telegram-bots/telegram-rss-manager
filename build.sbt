name := "telegram-rss-manager"

version := "0.0.1"

scalaVersion := "2.12.4"

libraryDependencies ++= {
  val akkaVersion = "2.5.11"
  val slickVersion = "3.2.1"
  val macWireVersion = "2.3.0"

  Seq(
    // Core
    "com.typesafe.akka" %% "akka-actor"                           % akkaVersion,
    "com.typesafe.akka" %% "akka-stream"                          % akkaVersion,
    "com.typesafe.akka" %% "akka-remote"                          % akkaVersion,
    "com.typesafe.akka" %% "akka-http"                            % "10.0.11",

    // Logging
    "com.typesafe.akka" %% "akka-slf4j"                           % akkaVersion,
    "ch.qos.logback"    %  "logback-classic"                      % "1.2.3",

    // DB
    "com.typesafe.slick" %% "slick"                               % slickVersion,
    "com.typesafe.slick" %% "slick-hikaricp"                      % slickVersion,
    "org.postgresql"     %  "postgresql"                          % "42.2.1",

    // IOC
    "com.softwaremill.macwire" %% "macros"                        % macWireVersion % "provided",
    "com.softwaremill.macwire" %% "macrosakka"                    % macWireVersion % "provided",
    "com.softwaremill.macwire" %% "util"                          % macWireVersion,
    "com.softwaremill.macwire" %% "proxy"                         % macWireVersion,

    "net.ruippeixotog" %% "scala-scraper"                         % "2.1.0",
    "org.scala-lang.modules" %% "scala-xml"                       % "1.1.0",

    // Testing
    "org.scalatest" %% "scalatest" % "3.0.5" % Test
  )
}