package vocaradio

import akka.http.scaladsl.server.HttpApp

object WebServer extends HttpApp {
  override def routes =
    get {
      pathSingleSlash {
        complete(IndexView())
      }
    } ~ pathPrefix("assets") {
      getFromResourceDirectory("assets")
    }

  def main(args: Array[String]): Unit = {
    WebServer.startServer("0.0.0.0", 8080)
  }
}
