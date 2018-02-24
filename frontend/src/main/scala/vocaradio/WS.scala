package vocaradio

import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import org.scalajs.dom._
import scala.scalajs.js.timers._
import scala.concurrent.duration._

object WS {
  var underlying: WebSocket = null

  val url = window.location.origin.get
    .replaceFirst("http", "ws")
    .+("/connect")

  def init(retrySeconds: FiniteDuration = 1.second)(
      msgHandler: VocaMessage => Unit
  ): Unit = {
    def closeHandler(d: FiniteDuration) = { e: CloseEvent =>
      println(s"WebSocket closed. Reconnect in ${d.toSeconds} seconds.")
      setTimeout(d)(init((d * 2) min 500.days)(msgHandler))
    }
    underlying = new WebSocket(url)
    underlying.onopen = { e =>
      println("WebSocket connected.")
      underlying.onclose = closeHandler(1.second)
    }
    underlying.onmessage = { e =>
      decode[VocaMessage](e.data.toString).foreach { msg =>
        msgHandler(msg)
      }
    }
    underlying.onclose = closeHandler(retrySeconds)
  }

  def send(msg: VocaMessage) = {
    underlying.send(msg.asJson.noSpaces)
  }
}
