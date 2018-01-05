package vocaradio

import scalacss.DevDefaults._

object CSS extends StyleSheet.Inline {
  import dsl._

  val leftPanel = style(
    position.fixed,
    top.`0`,
    left.`0`,
    width(23.%%),
    height(100.vh),
    backgroundImage := "url('/assets/img/sur_les_nuages.jpg')",
    backgroundPosition := "center",
    backgroundSize := "cover"
  )

  val middlePanel = style(
    position.relative,
    left(23.%%),
    width(54.%%),
    minHeight(100.vh),
    backgroundColor(c"#3d3d3d"),
    boxShadow := "0px 0px 10px black",
  )

  val rightPanel = style(
    position.fixed,
    top.`0`,
    right.`0`,
    width(23.%%),
    height(100.vh),
    backgroundColor(c"#1e1e1e")
  )

  val logo = style(
    fontFamily :=! "'Oswald', sans-serif",
    fontSize(4.3.rem),
    textAlign.center
  )

  val fixRatioWrapper = style(
    position.relative,
    height.`0`,
    width(100.%%),
    paddingTop(56.25.%%),
    overflow.hidden
  )

  val fixRatioItem = style(
    position.absolute,
    top.`0`,
    left.`0`,
    width(100.%%),
    height(100.%%)
  )

  val iframe = style(
    border.`0`
  )
}
