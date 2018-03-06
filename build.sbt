name := "telegram-rss-manager"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies ++= {
  Seq(
    // Core
    "com.typesafe.akka" %% "akka-actor"                           % "2.5.11",
    "com.typesafe.akka" %% "akka-stream"                          % "2.5.11",
    "com.typesafe.akka" %% "akka-remote"                          % "2.5.11",
    "com.typesafe.akka" %% "akka-http"                            % "10.0.11",

    // Logging
    "com.typesafe.akka" %% "akka-slf4j"                           % "2.5.11",
    "ch.qos.logback" % "logback-classic"                          % "1.2.3",

    "net.ruippeixotog" %% "scala-scraper"                         % "2.1.0",
    "commons-validator" % "commons-validator"                     % "1.5+"
  )
}