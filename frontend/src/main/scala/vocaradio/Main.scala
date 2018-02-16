package vocaradio

import org.scalajs.dom._
import org.scalajs.dom.raw._
import scala.language.implicitConversions
import scala.scalajs.js
import scalatags.JsDom.all._
import scalatags.JsDom.TypedTag
import scalacss.ScalatagsCss._
import scalacss.DevDefaults._

object Main {
  implicit class RichHTMLElement(h: HTMLElement) {
    def hide() = h.asInstanceOf[js.Dynamic].hidden = true
    def show() = h.asInstanceOf[js.Dynamic].hidden = false
  }

  def main(args: Array[String]): Unit = {
    println("Welcome to VocaRadio!")

    val portalName = input(
      CSS.portalName,
      CSS.textarea
    ).render
    portalName.hide()

    val portalBtn = a(
      CSS.btn,
      CSS.portalBtn,
      href := "/login", "登入"
    ).render

    val playerControl = {
      div(
        display := "none",
        paddingTop := "50px",
        paddingBottom := "50px",
        input(),
        div(
          button(
            CSS.btn,
            onclick := { () =>

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
            src := "https://www.youtube.com/embed/xYVcxsuj0PI?rel=0"
          )
        ),
        playerControl
      ),
      div(
        CSS.rightPanel,
        div(CSS.portal, portalName, portalBtn)
      )
    ).render

    document.querySelector("head")
      .appendChild(CSS.render[TypedTag[HTMLStyleElement]].render)
    document.getElementById("root")
      .appendChild(root)

    WS.init() {
      case UserStatus(isLoggedIn, isAdmin) =>
        if (isLoggedIn) {
          portalName.show()
          portalBtn.setAttribute("href", "/logout")
          portalBtn.textContent = "登出"
          if (isAdmin) playerControl.style.display = "block"
        } else {
        }
      case _ => //TODO
    }
  }
}
