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
    backgroundSize := "cover",
    media.maxWidth(700.px)(
      display.none
    )
  )

  val middlePanel = style(
    position.relative,
    marginLeft(23.%%),
    width(54.%%),
    minHeight(100.vh),
    backgroundColor(c"#3d3d3d"),
    boxShadow := "0px 0px 10px black",
    media.maxWidth(700.px)(
      zIndex(1),
      marginLeft.`0`,
      width(100.%%)
    )
  )

  val rightPanel = style(
    boxSizing.borderBox,
    position.fixed,
    top.`0`,
    right.`0`,
    width(23.%%),
    height(100.vh),
    backgroundColor(c"#1e1e1e"),
    media.maxWidth(700.px)(
      zIndex(0),
      paddingLeft(15.px),
      width(100.%%)
    )
  )

  val logo = style(
    fontFamily :=! "'Oswald', sans-serif",
    fontSize(4.3.rem),
    textAlign.center,
    media.maxWidth(700.px)(
      fontSize(3.rem)
    )
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

  val queue = style(
    display.flex,
    flexWrap.wrap,
    overflow.hidden,
    padding(3.px)
  )

  val queueItemWrapper = style(
    position.relative,
    height.`0`,
    width(25.%%),
    paddingTop(18.75.%%)
  )

  val queueItem = style(
    position.absolute,
    top(3.px),
    left(3.px),
    width :=! "calc(100% - 6px)",
    height :=! "calc(100% - 6px)"
  )

  val playerControl = style(
    padding(1.rem, 0.5.rem)
  )

  val uploadLabel = style(
    marginRight(0.5.rem)
  )

  val portal = style(
    display.flex,
    padding(0.4.rem),
  )

  val portalName = style(
    flexGrow(3),
    marginRight(0.4.rem),
  )

  val portalBtn = style(
    flexGrow(1)
  )

  val textarea = style(
    color(c"#eaeaea"),
    backgroundColor(c"#2d2d2d"),
    border.none,
    fontSize(1.rem),
    padding(0.5.rem),
    outline.none,
    boxSizing.borderBox,
    resize.none,
    borderRadius(0.25.rem)
  )

  val btn = style(
    padding(0.375.rem, 0.75.rem, 0.375.rem, 0.75.rem),
    display.inlineBlock,
    textAlign.center,
    textDecoration := "none",
    fontSize(1.rem),
    lineHeight(1.5.rem),
    color(c"#eaeaea"),
    backgroundColor(c"#2d2d2d"),
    cursor.pointer,
    transition := "all .15s",
    border.none,
    borderRadius(0.25.rem),
    outline.none,
    &.hover(
      backgroundColor(c"#d9534f"),
      color(c"#ffffff")
    ),
    &.active(
      backgroundColor(c"#c9302c")
    )
  )
}
