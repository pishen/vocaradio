package vocaradio

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import scalacss.DevDefaults._
import scalacss.ScalatagsCss._
import scalatags.Text.TypedTag
import scalatags.Text.all._
import scalatags.Text.tags2

object IndexView {
  def apply() = HttpEntity(
    ContentTypes.`text/html(UTF-8)`,
    "<!DOCTYPE html>" + html(
      IndexStyles.html,
      head(
        meta(charset := "UTF-8"),
        meta(
          name := "viewport",
          content := "width=device-width,initial-scale=1"
        ),
        tags2.title("VocaRadio - 24hr VOCALOID 自動電台"),
        link(rel := "icon", href := "/assets/img/favicon.png"),
        IndexStyles.render[TypedTag[String]]
      ),
      body(
        IndexStyles.body,
        div(
          IndexStyles.leftPanel
        ),
        div(
          IndexStyles.middlePanel,
          div(
            IndexStyles.logo,
            "VocaRadio"
          ),
          div(
            IndexStyles.playerWrapper,
            iframe(
              IndexStyles.player,
              src := "https://www.youtube.com/embed/AS4q9yaWJkI?rel=0"
            )
          ),
          h3(
            marginLeft := "16px",
            "即將播放"
          ),
          div(
            IndexStyles.playlist
          )
        ),
        div(
          IndexStyles.rightPanel
        ),
        script(src := "/assets/js/vocaradio-jsdeps.js"),
        script(src := "/assets/js/vocaradio-fastopt.js")
      )
    )
  )
}
