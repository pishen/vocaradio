package vocaradio

import org.scalajs.dom.document
import scalatags.JsDom.all._

object Main {
  def main(args: Array[String]): Unit = {
    println("Welcom to VocaRadio!")

    val root = div(
      div(cls := "left-panel"),
      div(
        cls := "middle-panel",
        div(cls := "logo", "VocaRadio"),
        div(
          cls := "fix-ratio-wrapper",
          iframe(
            cls := "fix-ratio-item",
            src := "https://www.youtube.com/embed/o1iz4L-5zkQ"
          )
        )
      ),
      div(cls := "right-panel")
    ).render

    document.getElementById("root")
      .appendChild(root)
  }
}
