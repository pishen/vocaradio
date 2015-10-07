lazy val root = (project in file(".")).enablePlugins(PlayScala).settings(
  name := "vocaradio",
  version := "0.1.0",
  scalaVersion := "2.11.7",
  libraryDependencies ++= Seq(
    ws,
    filters,
    "com.github.nscala-time" %% "nscala-time" % "2.2.0",
    "com.github.pathikrit" %% "better-files" % "2.3.0"
  ),
  resolvers += Resolver.bintrayRepo("pathikrit", "maven"),
  routesGenerator := InjectedRoutesGenerator
)
