val scalatest = "org.scalatest" % "scalatest_2.11" % "2.2.6" % "test"
val logging_library = "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0"
val slf4j = "org.slf4j" % "slf4j-api" % "1.7.21"
val logback_core = "ch.qos.logback" % "logback-core" % "1.1.7"
val logback_classic = "ch.qos.logback" % "logback-classic" % "1.1.7"
val junit = "junit" % "junit" % "4.12" % "test"
val scopt = "com.github.scopt" %% "scopt" % "3.4.0"
val gstlib = "com.github.guillaumedd" %% "gstlib" % "0.1.2"

lazy val commonSettings = Seq(
  organization := "fr.isir",
  version := "0.1",
  scalaVersion := "2.11.11"
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

scalacOptions ++= Seq("-deprecation", "-Ywarn-unused-import",  "-Ywarn-unused", "-Ywarn-dead-code", "-optimize")

assemblyJarName in assembly := "dialign.jar"
mainClass in assembly := Some("dialign.app.DialogueLexiconExporterApp")
