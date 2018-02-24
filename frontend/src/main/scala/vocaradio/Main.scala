package vocaradio

import io.circe.parser._
import io.circe.generic.auto._
import org.scalajs.dom._
import org.scalajs.dom.raw._
import scala.language.implicitConversions
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import scalatags.JsDom.all._
import scalatags.JsDom.TypedTag
import scalacss.ScalatagsCss._
import scalacss.DevDefaults._

@js.native
@JSGlobal("YT.Player")
class Player(id: String, config: js.Object) extends js.Object {
  def cueVideoById(videoId: String): Unit = js.native
  def loadVideoById(videoId: String, startSeconds: Int): Unit = js.native
}

@js.native
@JSGlobal("YT.PlayerState")
object PlayerState extends js.Any {
  val ENDED: Int = js.native
  val PLAYING: Int = js.native
}

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
            id := "player",
            CSS.fixRatioItem,
            border := "0",
            src := "https://www.youtube.com/embed/init?rel=0&enablejsapi=1"
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

    var sendResume = true
    val player = new Player("player", js.Dynamic.literal(
      events = js.Dynamic.literal(
        onReady = { (event: js.Any) =>
          println("Player ready")
          WS.send(Ready)
        },
        onStateChange = { (event: js.Dynamic) =>
          event.data.asInstanceOf[Int] match {
            case PlayerState.PLAYING =>
              if (sendResume) {
                println("PLAYING (sent)")
                WS.send(Resume)
              } else {
                println("PLAYING (not sent)")
                sendResume = true
              }
            case PlayerState.ENDED =>
              println("ENDED")
              WS.send(Ended)
            case _ =>
              // unused
          }
        }
      )
    ))

    WS.init() {
      case UserStatus(isLoggedIn, isAdmin) =>
        if (isLoggedIn) {
          portalName.show()
          portalBtn.setAttribute("href", "/logout")
          portalBtn.textContent = "登出"
          if (isAdmin) playerControl.show()
        }
      case SongAdded(query) =>
        uploadedCount += 1
        uploadMessage.textContent = s"$uploadedCount songs added."
      case Load(id) =>
        println("Load")
        player.cueVideoById(id)
      case Play(id, at) =>
        println("Play")
        sendResume = false
        player.loadVideoById(id, at)
      case _ => //TODO
    }
  }
}
