name := "vocaradio"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "com.github.scala-incubator.io" %% "scala-io-core" % "0.4.2",
  "org.anormcypher" %% "anormcypher" % "0.4.4"
)

resolvers ++= Seq(
  "anormcypher" at "http://repo.anormcypher.org/",
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
)

scalacOptions ++= Seq("-deprecation", "-feature")

play.Project.playScalaSettings
