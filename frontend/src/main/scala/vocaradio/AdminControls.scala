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
          .foreach(song => WS.send(BatchSave(song)))
      }
    }
  ).render

  val uploadMessage = span(CSS.adminControl).render

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

    val saveBtn = button(
      CSS.adminControl,
      CSS.btn,
      onclick := { () =>
        if (queryInput.value != "") {
          WS.send(
            Save(
              Song(queryInput.value, Some(idInput.value).filter(_ != ""), true)
            )
          )
        }
      },
      "Save"
    ).render

    val deleteBtn = button(
      CSS.adminControl,
      CSS.btn,
      onclick := { () => WS.send(Delete(queryInput.value)) },
      "Delete"
    ).render

    val element = div(
      CSS.adminControlRow,
      queryInput,
      idInput,
      saveBtn,
      deleteBtn
    ).render
  }

  // TODO: Try to eliminate the mutables here
  var songForms = Seq(SongForm("", ""))
  val songFormsDiv = div(songForms.map(_.element)).render

  def showSongs(songs: Seq[Song]) = {
    songForms = songs.map {
      song => SongForm(song.query, song.idOpt.getOrElse(""))
    }
    if (songForms.isEmpty) {
      songForms = Seq(SongForm("", ""))
    }
    songFormsDiv.innerHTML = ""
    songForms.foreach(songForm => songFormsDiv.appendChild(songForm.element))
  }

  def batchSaved(qeury: String) = {
    uploadedCount += 1
    uploadMessage.textContent = s"$uploadedCount songs saved."
  }

  def saved(query: String) = {
    songForms
      .filter(_.queryInput.value == query)
      .foreach(_.element.asInstanceOf[js.Dynamic].remove())
    songForms = songForms.filterNot(_.queryInput.value == query)
    if (songForms.isEmpty) {
      songForms = Seq(SongForm("", ""))
      songFormsDiv.appendChild(songForms.head.element)
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
    songFormsDiv
  ).render
  element.hide()


}
