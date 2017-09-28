package vocaradio

import scalacss.DevDefaults._

object IndexStyles extends StyleSheet.Inline {
  import dsl._

  val html = style(
    height(100.%%)
  )

  val body = style(
    height(100.%%),
    margin(0.px),
    display.flex
  )

  val leftPanel = style(
    flexGrow(2),
    backgroundImage := "url('/assets/img/neige.jpg')",
    backgroundPosition := "center",
    backgroundSize := "cover"
  )

  val middlePanel = style(
    flexGrow(5),
    flexBasis := "0",
    backgroundColor(c"#3d3d3d"),
    boxShadow := "0px 0px 10px black",
    overflowY.auto
  )

  val logo = style(
    textAlign.center,
    fontSize(55.px),
    padding(15.px, 0.px, 15.px, 0.px),
    color(c"#eaeaea"),
    fontFamily := "'Chelsea Market', cursive"
  )

  val playerWrapper = style(
    position.relative,
    height(0.px),
    overflow.hidden,
    width(100.%%),
    paddingTop((56.25).%%)
  )

  val player = style(
    position.absolute,
    top.`0`,
    left.`0`,
    border.`0`,
    width(100.%%),
    height(100.%%)
  )

  val playlist = style(
    display.flex,
    flexWrap.wrap
  )

  val playlistItemWrapper = style(
    position.relative,
    height(0.px),
    overflow.hidden,
    width(200.px),
    paddingTop((56.25).%%),
    flexGrow(1)
  )

  val playlistItem = style(
    position.absolute,
    top.`0`,
    left.`0`,
    border.`0`,
    width(100.%%),
    height(100.%%)
  )

  val rightPanel = style(
    flexGrow(2),
    backgroundColor(c"#1e1e1e")
  )
}
