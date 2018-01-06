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

  val textarea = style(
    width(100.%%),
    color(c"#eaeaea"),
    backgroundColor(c"#2d2d2d"),
    border.none,
    fontSize(1.rem),
    padding(0.5.rem),
    outline.none,
    boxSizing.borderBox,
    resize.vertical
  )

  val btnGroup = style(
    display.flex,
    borderBottom(1.px, solid, c"#868686")
  )

  val btn = style(
    flexGrow(1),
    paddingTop(0.5.rem),
    paddingBottom(0.5.rem),
    textAlign.center,
    textDecoration := "none",
    fontSize(1.rem),
    color(c"#eaeaea"),
    background := "none",
    cursor.pointer,
    transition := "all .15s",
    borderTop.none,
    borderRight.none,
    borderBottom.none,
    borderLeft(1.px, solid, c"#868686"),
    outline.none,
    &.firstChild(
      borderLeft.none
    ),
    &.hover(
      backgroundColor(c"#eaeaea"),
      color(c"#333333")
    )
  )
}
