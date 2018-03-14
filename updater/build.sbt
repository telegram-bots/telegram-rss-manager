name := Build.namePrefix + "updater"

version := "0.0.1"

libraryDependencies ++= {
  Seq(
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

    // IOC
    "com.softwaremill.macwire" %% "macros"                        % Dependencies.macWireVersion % "provided",
    "com.softwaremill.macwire" %% "macrosakka"                    % Dependencies.macWireVersion % "provided",
    "com.softwaremill.macwire" %% "util"                          % Dependencies.macWireVersion,
    "com.softwaremill.macwire" %% "proxy"                         % Dependencies.macWireVersion,

    "net.ruippeixotog" %% "scala-scraper"                         % "2.1.0",

    // Tests
    "com.typesafe.akka" %% "akka-testkit"                         % Dependencies.akkaVersion % Test
  )
}