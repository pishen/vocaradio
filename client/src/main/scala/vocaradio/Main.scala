package vocaradio

import org.scalajs.dom._
import org.scalajs.dom.raw.HTMLStyleElement
import scalatags.JsDom.all._
import scalatags.JsDom.TypedTag
import scalacss.ScalatagsCss._
import scalacss.DevDefaults._

object Main {
  def main(args: Array[String]): Unit = {
    println("Welcome to VocaRadio!")

    val playerControl = div(
      display := "none",
      div(
        textarea(rows := "10")
      )
    ).render

    val root = div(
      div(CSS.leftPanel),
      div(
        CSS.middlePanel,
        div(CSS.logo, "VocaRadio"),
        div(
          CSS.fixRatioWrapper,
          iframe(
            CSS.fixRatioItem,
            CSS.iframe,
            src := "https://www.youtube.com/embed/kWJ4z-I25LU?rel=0"
          )
        ),
        playerControl
      ),
      div(
        CSS.rightPanel,
        div(
          CSS.btnGroup,
          a(
            CSS.btn,
            href := "/login",
            "登入"
          )
        )
      )
    ).render

    document.querySelector("head")
      .appendChild(CSS.render[TypedTag[HTMLStyleElement]].render)
    document.getElementById("root")
      .appendChild(root)

    WS.init() {
      case TurnOnPlayerControl =>
        playerControl.style.display = "block"
    }
  }
}
