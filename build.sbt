name := "vocaradio"

version in ThisBuild := "2.0.0-SNAPSHOT"

scalaVersion in ThisBuild := "2.12.3"

libraryDependencies ++= Seq(
  "com.github.japgolly.scalacss" %% "core" % "0.5.3",
  "com.github.japgolly.scalacss" %% "ext-scalatags" % "0.5.3",
  "com.lihaoyi" %% "scalatags" % "0.6.5",
  "com.typesafe.akka" %% "akka-http" % "10.0.10"
)

resourceGenerators in Compile += Def.task {
  val indexJs = {
    val src = (fastOptJS in Compile in jsIndex).value.data
    val target = (resourceManaged in Compile).value / "assets" / "js" / src.getName
    IO.copyFile(src, target)
    target
  }
  Seq(indexJs)
}

lazy val jsIndex = (project in file("js/index"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "index",
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "scalatags" % "0.6.5",
      "org.scala-js" %%% "scalajs-dom" % "0.9.1"
    )
  )
