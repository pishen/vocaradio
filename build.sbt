name := "vocaradio"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache
)

scalacOptions ++= Seq("-deprecation", "-feature")

play.Project.playScalaSettings
