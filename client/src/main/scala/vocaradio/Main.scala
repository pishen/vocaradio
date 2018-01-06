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

    val guestPortal = div(
      CSS.btnGroup,
      display := "none",
      a(
        CSS.btn,
        href := "/login",
        "登入"
      )
    ).render

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
            src := "https://www.youtube.com/embed/o1iz4L-5zkQ?rel=0"
          )
        ),
        playerControl
      ),
      div(
        CSS.rightPanel,
        guestPortal
      )
    ).render

    document.querySelector("head")
      .appendChild(CSS.render[TypedTag[HTMLStyleElement]].render)
    document.getElementById("root")
      .appendChild(root)

    WS.init() {
      case UserStatus(isLoggedIn, isAdmin) =>
        if (!isLoggedIn) {
          guestPortal.style.display = "flex"
        }
    }
  }
}
