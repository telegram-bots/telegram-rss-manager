name := Build.namePrefix + "bot"

version := "0.0.1"

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