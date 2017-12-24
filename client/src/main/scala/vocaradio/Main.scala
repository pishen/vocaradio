package vocaradio

import org.scalajs.dom._
import scalatags.JsDom.all._

object Main {
  def main(args: Array[String]): Unit = {
    println("Welcome to VocaRadio!")

    val root = div(
      div(cls := "left-panel"),
      div(
        cls := "middle-panel",
        div(cls := "logo", "VocaRadio"),
        div(
          cls := "fix-ratio-wrapper",
          iframe(
            cls := "fix-ratio-item",
            src := "https://www.youtube.com/embed/hA0W2lJikso?rel=0"
          )
        )
      ),
      div(cls := "right-panel")
    ).render

    document.getElementById("root")
      .appendChild(root)

    WS.init() {
      case TurnOnPlayerControl => println("haha")
    }
  }
}
