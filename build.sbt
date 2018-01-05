val scalaV = "2.12.4"

val commonSettings = Seq(
  name := "vocaradio",
  version := "2.0.0-SNAPSHOT",
  scalaVersion := scalaV
)

lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared"))
  .settings(scalaVersion := scalaV)
lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

lazy val server = (project in file("server"))
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      //logging
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.typesafe.akka" %% "akka-slf4j" % "2.4.20",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
      //database
      "com.h2database" % "h2" % "1.4.196",
      "com.typesafe.slick" %% "slick" % "3.2.1",
      "com.typesafe.slick" %% "slick-hikaricp" % "3.2.1",
      //json
      "io.circe" %% "circe-core" % "0.8.0",
      "io.circe" %% "circe-generic" % "0.8.0",
      "io.circe" %% "circe-parser" % "0.8.0",
      "de.heikoseeberger" %% "akka-http-circe" % "1.18.0",
      //akka-http
      "com.typesafe.akka" %% "akka-http" % "10.0.11",
      "com.softwaremill.akka-http-session" %% "core" % "0.5.1"
    ),
    resourceGenerators in Compile += Def.task {
      Seq(
        (fastOptJS in Compile in client).value.data,
        (packageJSDependencies in Compile in client).value
      ).map { f =>
        val dest = (resourceManaged in Compile).value / "assets" / "js" / f.name
        IO.copyFile(f, dest)
        dest
      }
    }
  )
  .dependsOn(sharedJvm)

lazy val client = (project in file("client"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    commonSettings,
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.2",
      "com.lihaoyi" %%% "scalatags" % "0.6.5",
      "com.github.japgolly.scalacss" %%% "ext-scalatags" % "0.5.3",
      // json
      "io.circe" %%% "circe-core" % "0.8.0",
      "io.circe" %%% "circe-generic" % "0.8.0",
      "io.circe" %%% "circe-parser" % "0.8.0"
    )
  )
  .dependsOn(sharedJs)

lazy val root = (project in file("."))
  .settings(commonSettings)
  .aggregate(server, client)
