package vocaradio

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import scalatags.Text.TypedTag

trait ViewHelper {
  def wrap(html: TypedTag[String]) = {
    HttpEntity(ContentTypes.`text/html(UTF-8)`, "<!DOCTYPE html>" + html)
  }
}
