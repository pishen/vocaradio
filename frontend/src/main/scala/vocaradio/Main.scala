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
  def getVideoData(): VideoData = js.native
  def getCurrentTime(): Double = js.native
  def seekTo(seconds: Int, allowSeekAhead: Boolean): Unit = js.native
}

@js.native
trait VideoData extends js.Object {
  val video_id: String = js.native
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

    val middlePanel = div(
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
    ).render

    var touchX: Int = 0
    var opening: Boolean = false

    middlePanel.addEventListener("touchstart", { e: TouchEvent =>
      middlePanel.style.transition = "none"
      touchX = e.touches(0).clientX.toInt
    })

    middlePanel.addEventListener("touchmove", { e: TouchEvent =>
      val clientX = e.touches(0).clientX.toInt
      val offset = clientX - touchX
      touchX = clientX
      opening = offset < 0
      val leftLimit = -middlePanel.clientWidth + 15
      val updatedOffset = middlePanel.offsetLeft + offset
      middlePanel.style.left = (updatedOffset max leftLimit min 0) + "px"
    })

    middlePanel.addEventListener("touchend", { e: TouchEvent =>
      middlePanel.style.transition = "left 0.5s"
      middlePanel.style.left = if (opening) {
        (-middlePanel.clientWidth + 15) + "px"
      } else "0px"
    })

    val rightPanel = div(
      CSS.rightPanel,
      div(CSS.portal, portalName, portalBtn)
    ).render

    val root = div(
      div(CSS.leftPanel),
      middlePanel,
      rightPanel
    ).render

    document.querySelector("head")
      .appendChild(CSS.render[TypedTag[HTMLStyleElement]].render)
    document.getElementById("root")
      .appendChild(root)

    val player = new Player("player", js.Dynamic.literal(
      events = js.Dynamic.literal(
        onReady = { (event: js.Any) =>
          println("Player ready")
          WS.send(Ready)
        },
        onStateChange = { (event: js.Dynamic) =>
          val player = event.target.asInstanceOf[Player]
          event.data.asInstanceOf[Int] match {
            case PlayerState.PLAYING =>
              println("PLAYING")
              WS.send(
                Resume(
                  player.getVideoData().video_id,
                  player.getCurrentTime().toInt
                )
              )
            case PlayerState.ENDED =>
              println("ENDED")
              WS.send(Ended)
            case _ =>
              //println(event.data)
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
        if (player.getVideoData().video_id == id) {
          player.seekTo(at, true)
        } else {
          player.loadVideoById(id, at)
        }
      case UpdatePlaylist(videos) =>
        println("UpdatePlaylist")
      case _ => //TODO
    }
  }
}
