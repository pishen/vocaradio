lazy val root = (project in file(".")).enablePlugins(PlayScala).settings(
  name := "vocaradio",
  version := "1.0",
  scalaVersion := "2.11.7",
  libraryDependencies ++= Seq(
    ws,
    filters,
    "com.github.nscala-time" %% "nscala-time" % "2.2.0",
    "com.github.pathikrit" %% "better-files" % "2.13.0",
    "net.ceedubs" %% "ficus" % "1.1.2"
  ),
  routesGenerator := InjectedRoutesGenerator
)
