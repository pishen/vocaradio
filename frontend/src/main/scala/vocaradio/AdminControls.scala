package vocaradio

import io.circe.parser._
import io.circe.generic.auto._
import org.scalajs.dom._
import scala.scalajs.js
import scalatags.JsDom.all._
import scalacss.ScalatagsCss._

object AdminControls {
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

  val uploadMessage = span(CSS.adminControl).render

  val element = div(
    div(
      CSS.adminControlRow,
      label(
        CSS.btn,
        CSS.adminControl,
        uploadBtn,
        "Upload Songs"
      ),
      uploadMessage
    ),
    div(
      CSS.adminControlRow,
      button(
        CSS.btn,
        CSS.adminControl,
        onclick := { () => WS.send(Drop) },
        "Drop"
      )
    )
  ).render
  element.hide()

  def songAdded() = {
    uploadedCount += 1
    uploadMessage.textContent = s"$uploadedCount songs added."
  }
}
