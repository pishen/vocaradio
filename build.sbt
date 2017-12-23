val commonSettings = Seq(
  name := "vocaradio",
  version := "2.0.0-SNAPSHOT",
  scalaVersion := "2.12.4"
)

lazy val server = (project in file("server"))
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      //logging
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.typesafe.akka" %% "akka-slf4j" % "2.4.20",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
      //html & css
      "com.lihaoyi" %% "scalatags" % "0.6.5",
      "com.github.japgolly.scalacss" %% "core" % "0.5.3",
      "com.github.japgolly.scalacss" %% "ext-scalatags" % "0.5.3",
      //database
      "com.h2database" % "h2" % "1.4.196",
      "com.typesafe.slick" %% "slick" % "3.2.1",
      "com.typesafe.slick" %% "slick-hikaricp" % "3.2.1",
      //json
      "com.typesafe.play" %% "play-json" % "2.6.7",
      "de.heikoseeberger" %% "akka-http-play-json" % "1.18.0",
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

lazy val client = (project in file("client"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    commonSettings,
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.2",
      "com.lihaoyi" %%% "scalatags" % "0.6.5"
    )
  )
