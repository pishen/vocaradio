package vocaradio

import org.scalajs.dom._
import scalatags.JsDom.all._

object Main {
  def main(args: Array[String]): Unit = {
    println("Welcom to VocaRadio!")

    val wsUrl = window
      .location
      .origin
      .get
      .replaceFirst("http", "ws")
      .+("/connect")
    println(wsUrl)
    val ws = new WebSocket(wsUrl)

    val root = div(
      div(cls := "left-panel"),
      div(
        cls := "middle-panel",
        div(cls := "logo", "VocaRadio"),
        div(
          cls := "fix-ratio-wrapper",
          iframe(
            cls := "fix-ratio-item",
            src := "https://www.youtube.com/embed/OuLZlZ18APQ"
          )
        )
      ),
      div(cls := "right-panel")
    ).render

    document.getElementById("root")
      .appendChild(root)
  }
}
