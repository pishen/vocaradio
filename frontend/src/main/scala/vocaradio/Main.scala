package vocaradio

import io.circe.parser._
import io.circe.generic.auto._
import org.scalajs.dom._
import org.scalajs.dom.raw._
import scala.concurrent.Future
import scala.language.implicitConversions
import scala.scalajs.js
import scalatags.JsDom.all._
import scalatags.JsDom.TypedTag
import scalacss.ScalatagsCss._
import scalacss.DevDefaults._

object Main {
  def main(args: Array[String]): Unit = {
    println("Welcome to VocaRadio!")

    // queue
    val queue = Seq.fill(24)(div(CSS.videoHeightWrapper).render)

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
      hr(CSS.separator, data.content := "即將播放"),
      div(
        CSS.queue,
        queue.map(heightWrapper => div(CSS.videoWidthWrapper, heightWrapper))
      ),
      AdminControls.element
    ).render

    // touch detection on mobile
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
    // // // // //

    val rightPanel = div(
      CSS.rightPanel,
      Portal.element
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
          Portal.login()
          if (isAdmin) AdminControls.element.show()
        }
      case Saved(query) =>
        AdminControls.songSaved(query)
      case ShowSongs(songs) =>
        AdminControls.showSongs(songs)
      case Load(id) =>
        println("Load")
        player.cueVideoById(id)
      case Play(id, position) =>
        println("Play")
        if (player.getVideoData().video_id == id) {
          player.seekTo(position, true)
        } else {
          player.loadVideoById(id, position)
        }
      case UpdatePlaylist(pickables) =>
        println("UpdatePlaylist")

        // clean the trailing pickables if pickables is too short
        queue.drop(pickables.size).foreach(_.innerHTML = "")

        val oldIds = queue
          .flatMap(_.childrenSeq.lastOption)
          .map(_.getAttribute("data-video-id"))
          .toSet
        // if there's no new songs, the direction must be right
        val shiftRight = oldIds == pickables.map(_.video.id).toSet

        queue.zip(pickables)
          .foreach { case (heightWrapper, Pickable(video, pickerOpt)) =>
            val needShift = heightWrapper.childrenSeq
              .lastOption
              .map(_.getAttribute("data-video-id") != video.id)
              .getOrElse(true)
            if (needShift) {
              // remove old one
              heightWrapper.childrenSeq.lastOption.foreach { elem =>
                elem.classList.remove(CSS.fromRight.htmlClass)
                elem.classList.remove(CSS.fromLeft.htmlClass)
                elem.classList.add {
                  if (shiftRight) {
                    CSS.toRight.htmlClass
                  } else {
                    CSS.toLeft.htmlClass
                  }
                }
                elem.addEventListener("animationend", { e: Event =>
                  heightWrapper.removeChild(elem)
                })
              }
              // add new one
              heightWrapper.appendChild {
                div(
                  CSS.video,
                  if (shiftRight) CSS.fromLeft else CSS.fromRight,
                  data.video.id := video.id,
                  backgroundImage := {
                    s"url('${video.snippet.thumbnails.medium.url}')"
                  },
                  div(
                    CSS.videoCover,
                    a(
                      CSS.videoLink,
                      href := s"https://www.youtube.com/watch?v=${video.id}",
                      target := "_blank",
                      video.snippet.title
                    )
                  )
                ).render
              }
            }
          }
      case _ => //TODO
    }
  }
}
