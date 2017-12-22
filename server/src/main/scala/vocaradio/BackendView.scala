package vocaradio

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import scalatags.Text.all._
import scalatags.Text.tags2

object BackendView {
  def apply() = HttpEntity(
    ContentTypes.`text/html(UTF-8)`,
    "<!DOCTYPE html>" + html(
      head(
        meta(charset := "UTF-8"),
        meta(
          name := "viewport",
          content := "width=device-width,initial-scale=1"
        ),
        tags2.title("Backend"),
        link(rel := "icon", href := "/assets/img/favicon.png"),
        link(
          rel := "stylesheet",
          href := "//maxcdn.bootstrapcdn.com/bootstrap/4.0.0-beta.2/css/bootstrap.min.css"
        )
      ),
      body(
        div(
          cls := "container",
          h1(marginTop := "2rem", "VocaRadio Backend"),
          hr,
          form(
            div(
              cls := "form-group",
              textarea(cls := "form-control", rows := "3")
            ),
            button(cls := "btn btn-primary", "Import")
          )
        ),
        script(src := "//code.jquery.com/jquery-3.2.1.min.js"),
        script(src := "//cdnjs.cloudflare.com/ajax/libs/popper.js/1.12.3/umd/popper.min.js"),
        script(src := "//maxcdn.bootstrapcdn.com/bootstrap/4.0.0-beta.2/js/bootstrap.min.js")
      )
    )
  )
}
