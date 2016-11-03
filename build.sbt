organization := "com.ovoenergy"
scalaVersion := "2.11.8"

reformatOnCompileSettings
scalafmtConfig in ThisBuild := Some(file(".scalafmt.conf"))

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "0.7.2",
  "io.circe" %% "circe-core" % "0.5.4",
  "io.circe" %% "circe-parser" % "0.5.4",
  "io.circe" %% "circe-generic" % "0.5.4",
  "com.typesafe.akka" %% "akka-stream-kafka" % "0.12",
  "com.typesafe" % "config" % "1.3.1",
  "com.squareup.okhttp3" % "okhttp" % "3.4.1",
  "ch.qos.logback" % "logback-classic" % "1.0.9",
  "me.moocar" % "logback-gelf" % "0.2",
  "com.trueaccord.scalapb" %% "scalapb-json4s" % "0.1.2",
  "org.scalatest" %% "scalatest" % "3.0.0"
)

enablePlugins(JavaServerAppPackaging, DockerPlugin)
DockerPackage.settings

// ScalaPB code generation
PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

// Make ScalaTest write test reports that CirceCI understands
val testReportsDir = sys.env.getOrElse("CI_REPORTS", "target/reports")
testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-o", "-u", testReportsDir)
