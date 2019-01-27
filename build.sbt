val commonSettings = Seq(
  name := "vocaradio",
  version := "2.1.0",
  scalaVersion := "2.12.6"
)

lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared"))
  .settings(scalaVersion := "2.12.6")
lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

lazy val backend = (project in file("backend"))
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      //akka-http
      "com.typesafe.akka" %% "akka-http" % "10.1.1",
      "com.softwaremill.akka-http-session" %% "core" % "0.5.5",
      //logging
      "com.typesafe.akka" %% "akka-slf4j" % "2.5.12",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      //database
      "com.typesafe.slick" %% "slick" % "3.2.1",
      "com.typesafe.slick" %% "slick-hikaricp" % "3.2.1",
      "com.h2database" % "h2" % "1.4.196",
      //json
      "io.circe" %% "circe-core" % "0.9.3",
      "io.circe" %% "circe-generic" % "0.9.3",
      "io.circe" %% "circe-parser" % "0.9.3",
      "de.heikoseeberger" %% "akka-http-circe" % "1.20.1"
    ),
    resourceGenerators in Compile += Def.task {
      Seq(
        (fastOptJS in Compile in frontend).value.data,
        (packageJSDependencies in Compile in frontend).value
      ).map { f =>
        val dest = (resourceManaged in Compile).value / "assets" / "js" / f.name
        IO.copyFile(f, dest)
        dest
      }
    }
  )
  .dependsOn(sharedJvm)

lazy val frontend = (project in file("frontend"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    commonSettings,
    scalaJSUseMainModuleInitializer := true,
    emitSourceMaps := false,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.2",
      "com.lihaoyi" %%% "scalatags" % "0.6.5",
      "com.github.japgolly.scalacss" %%% "ext-scalatags" % "0.5.3",
      // json
      "io.circe" %%% "circe-core" % "0.9.3",
      "io.circe" %%% "circe-generic" % "0.9.3",
      "io.circe" %%% "circe-parser" % "0.9.3"
    )
  )
  .dependsOn(sharedJs)
