package vocaradio

import io.circe.parser._
import io.circe.generic.auto._
import org.scalajs.dom._
import org.scalajs.dom.raw.HTMLInputElement
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
          .flatMap(str => decode[Song](str).toOption)
          .foreach(song => WS.send(Save(song)))
      }
    }
  ).render

  val uploadMessage = span(CSS.adminControl).render

  def songSaved() = {
    uploadedCount += 1
    uploadMessage.textContent = s"$uploadedCount songs saved."
  }

  val idInput = input(
    CSS.adminControl,
    CSS.adminControlInput,
    placeholder := "id"
  ).render

  case class SongForm(query: String, id: String) {
    val queryInput = input(
      CSS.adminControl,
      CSS.adminControlInput,
      placeholder := "query",
      value := query
    ).render
    val idInput = input(
      CSS.adminControl,
      CSS.adminControlInput,
      placeholder := "id",
      value := id
    ).render
    val saveBtn = button(CSS.adminControl, CSS.btn, "Save").render
    val deleteBtn = button(CSS.adminControl, CSS.btn, "Delete").render

    val element = div(
      CSS.adminControlRow,
      queryInput,
      idInput,
      saveBtn,
      deleteBtn
    ).render
  }

  val songForms = div(
    SongForm("", "").element
  ).render

  def showSongs(songs: Seq[Song]) = {
    songForms.innerHTML = ""
    songs.foreach { song =>
      songForms.appendChild(
        SongForm(song.query, song.idOpt.getOrElse("")).element
      )
    }
  }

  val element = div(
    div(
      CSS.adminControlRow,
      button(
        CSS.btn,
        CSS.adminControl,
        onclick := { () => WS.send(Drop) },
        "Drop"
      )
    ),
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
      idInput,
      button(
        CSS.btn,
        CSS.adminControl,
        onclick := { () => WS.send(Id(idInput.value)) },
        "List"
      )
    ),
    songForms
  ).render
  element.hide()


}
