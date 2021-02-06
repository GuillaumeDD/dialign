val scalatest = "org.scalatest" %% "scalatest" % "3.1.1" % "test"
val logging_library = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
val slf4j = "org.slf4j" % "slf4j-api" % "1.7.21"
val logback_core = "ch.qos.logback" % "logback-core" % "1.1.7"
val logback_classic = "ch.qos.logback" % "logback-classic" % "1.1.7"
val junit = "junit" % "junit" % "4.12" % "test"
val scopt = "com.github.scopt" %% "scopt" % "3.7.1"
val gstlib = "com.github.guillaumedd" %% "gstlib" % "0.1.3"

lazy val commonSettings = Seq(
  organization := "com.github.guillaumedd",
  version := "2021.02",
  scalaVersion := "2.13.4"
)

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "dialign",
    libraryDependencies += scalatest,
    libraryDependencies += logging_library,
    libraryDependencies += slf4j,
    libraryDependencies += logback_core,
    libraryDependencies += logback_classic,
    libraryDependencies += junit,
    libraryDependencies += scopt,
    libraryDependencies += gstlib
  )

scalacOptions ++= Seq("-deprecation", "-Ywarn-unused", "-Ywarn-dead-code",
                      "-opt:l:inline", "-opt-inline-from:**", "-Ywarn-unused:imports")

//assemblyJarName in assembly := "dialign.jar"
//mainClass in assembly := Some("dialign.app.DialignOfflineApp")

assemblyJarName in assembly := "dialign-online.jar"
mainClass in assembly := Some("dialign.app.DialignOnlineApp")
