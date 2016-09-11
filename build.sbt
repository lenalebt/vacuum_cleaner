
organization := "de.lenabrueder"

name := """vacuum-cleaner"""

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.8"

val akkaVersion = "2.4.10"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,

  "org.scalanlp" %% "breeze" % "0.11.2"
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.6",
  "org.scalacheck" %% "scalacheck" % "1.12.5"
) map (_ % "test")

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

//settings to compile readme
tutSettings
tutSourceDirectory := baseDirectory.value / "tut"
tutTargetDirectory := baseDirectory.value