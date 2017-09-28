package vocaradio

import scalacss.DevDefaults._
import scalacss.ScalatagsCss._
import scalatags.Text.TypedTag
import scalatags.Text.all._
import scalatags.Text.tags2

object IndexView extends ViewHelper {
  def apply() = wrap {
    html(
      IndexStyles.html,
      head(
        meta(charset := "UTF-8"),
        meta(
          name := "viewport",
          content := "width=device-width, initial-scale=1"
        ),
        tags2.title("VocaRadio - 24hr VOCALOID 自動電台"),
        link(rel := "icon", href := "/assets/img/favicon.png"),
        link(rel := "stylesheet", href := "https://fonts.googleapis.com/css?family=Chelsea+Market"),
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
          div(
            IndexStyles.playlist,
            (1 to 25).map { i =>
              div(
                IndexStyles.playlistItemWrapper,
                img(
                  IndexStyles.playlistItem,
                  src := "https://i.ytimg.com/vi/OuLZlZ18APQ/mqdefault.jpg"
                )
              )
            }
          )
        ),
        div(
          IndexStyles.rightPanel
        )
      )
    )
  }
}
