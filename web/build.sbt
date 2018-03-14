name := Build.namePrefix + "web"

version := "0.0.1"

libraryDependencies ++= Seq(
  // Akka
  "com.typesafe.akka" %% "akka-actor"                           % Dependencies.akkaVersion,
  "com.typesafe.akka" %% "akka-stream"                          % Dependencies.akkaVersion,
  "com.typesafe.akka" %% "akka-remote"                          % Dependencies.akkaVersion,
  "com.typesafe.akka" %% "akka-http"                            % Dependencies.akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-xml"                        % Dependencies.akkaHttpVersion,

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

  "org.scala-lang.modules" %% "scala-xml"                       % "1.1.0",

  // Tests
  "com.typesafe.akka" %% "akka-testkit"                         % Dependencies.akkaVersion % Test
)

dockerfile in docker := {
  val jarFile: File = sbt.Keys.`package`.in(Compile, packageBin).value
  val classpath = (managedClasspath in Compile).value
  val mainclass = mainClass.in(Compile, packageBin).value.getOrElse(sys.error("Expected exactly one main class"))
  val jarTarget = s"/app/${jarFile.getName}"
  val classpathString = classpath.files.map("/app/" + _.getName)
    .mkString(":") + ":" + jarTarget

  new Dockerfile {
    from("openjdk:8-jre-alpine")
    add(classpath.files, "/app/")
    add(jarFile, jarTarget)
    entryPoint("java", "-cp", classpathString, mainclass)
  }
}

imageNames in docker := Seq(
  ImageName(
    namespace = Some(organization.value),
    repository = name.value,
    tag = Some("v" + version.value)
  )
)