name := "vocaradio"

version := "dev"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  ws,
  "com.github.scala-incubator.io" %% "scala-io-core" % "0.4.2",
  "org.neo4j"                     %  "neo4j"         % "2.0.1"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)
