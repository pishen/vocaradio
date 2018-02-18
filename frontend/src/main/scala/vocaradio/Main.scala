package vocaradio

import io.circe.parser._
import io.circe.generic.auto._
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

    var uploadedCount = 0

    val uploadBtn = input(
      `type` := "file",
      display := "none",
      onchange := { (e: js.Dynamic) =>
        uploadedCount = 0
        println("upload started")
        val f = e.target.files.asInstanceOf[FileList](0)
        val reader = new FileReader()
        reader.readAsText(f)
        reader.onloadend = { (e: ProgressEvent) =>
          reader.result
            .asInstanceOf[String]
            .split("\n")
            .flatMap(str => decode[AddSong](str).toOption)
            .foreach(WS.send)
        }
      }
    ).render

    val uploadMessage = span().render

    val playerControl = div(
      CSS.playerControl,
      label(
        CSS.btn,
        CSS.uploadLabel,
        uploadBtn,
        "Upload Songs"
      ),
      uploadMessage
    ).render
    playerControl.hide()

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
          if (isAdmin) playerControl.show()
        } else {
        }
      case SongAdded(query) =>
        uploadedCount += 1
        uploadMessage.textContent = s"$uploadedCount songs added."
      case _ => //TODO
    }
  }
}
