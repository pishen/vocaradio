name := "vocaradio"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache
)

libraryDependencies += "com.github.scala-incubator.io" %% "scala-io-core" % "0.4.2"

scalacOptions ++= Seq("-deprecation", "-feature")

play.Project.playScalaSettings
