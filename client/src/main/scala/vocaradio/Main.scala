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

    val userPortal = div(
      CSS.btnGroup,
      display := "none",
      a(
        CSS.btn,
        href := "/logout",
        "登出"
      )
    ).render

    val playerControl = {
      val food = textarea(
        CSS.textarea,
        placeholder := "Feed Me!",
        rows := "10"
      ).render
      div(
        display := "none",
        paddingTop := "50px",
        paddingBottom := "50px",
        food,
        div(
          CSS.btnGroup,
          button(
            CSS.btn,
            onclick := { () =>
              food.value.split("\n").foreach { str =>
                WS.send(AddSong(str, None))
              }
            },
            "送出"
          )
        )
      ).render
    }

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
            src := "https://www.youtube.com/embed/FT91CrPPAqc?rel=0"
          )
        ),
        playerControl
      ),
      div(
        CSS.rightPanel,
        guestPortal,
        userPortal
      )
    ).render

    document.querySelector("head")
      .appendChild(CSS.render[TypedTag[HTMLStyleElement]].render)
    document.getElementById("root")
      .appendChild(root)

    WS.init() {
      case UserStatus(isLoggedIn, isAdmin) =>
        if (isLoggedIn) {
          userPortal.style.display = "flex"
          if (isAdmin) playerControl.style.display = "block"
        } else {
          guestPortal.style.display = "flex"
        }
    }
  }
}
