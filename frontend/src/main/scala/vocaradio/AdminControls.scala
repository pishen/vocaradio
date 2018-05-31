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

  case class SongForm(var query: String, var id: String) {
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

    def setBtnState() = {
      deleteBtn.disabled = queryInput.value == ""
      saveBtn.disabled = queryInput.value == "" ||
        (queryInput.value == query && idInput.value == id)
      if (!saveBtn.disabled) {
        saveBtn.textContent = "Save"
      }
      if (!deleteBtn.disabled) {
        deleteBtn.textContent = "Delete"
      }
    }
    queryInput.onkeyup = { _ => setBtnState() }
    idInput.onkeyup = { _ => setBtnState() }
    setBtnState()

    def saved() = {
      query = queryInput.value
      id = idInput.value
      saveBtn.textContent = "Saved!"
      setBtnState()
    }

    def deleted() = {
      query = ""
      id = ""
      queryInput.value = ""
      idInput.value = ""
      deleteBtn.textContent = "Deleted!"
      setBtnState()
    }

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
      .foreach(_.saved())
  }

  def deleted(query: String) = {
    songForms
      .filter(_.queryInput.value == query)
      .foreach(_.deleted())
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
