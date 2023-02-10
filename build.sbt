import Dependencies._

ThisBuild / scalaVersion := "3.2.1"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

Runtime / unmanagedClasspath += baseDirectory.value / "src" / "main" / "resources"

lazy val root = (project in file("."))
  .settings(
    name := "hello-http",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.0.8",
      "io.github.ollls" %% "zio-tls-http" % "2.0.0",
    )
  )

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-no-indent"
)

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
