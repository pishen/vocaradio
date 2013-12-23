import play.Project._

name := "vocaradio"

version := "1.0"

libraryDependencies ++= Seq(
  "com.github.scala-incubator.io" %% "scala-io-core" % "0.4.2"
)

scalacOptions ++= Seq("-deprecation", "-feature")

playScalaSettings
