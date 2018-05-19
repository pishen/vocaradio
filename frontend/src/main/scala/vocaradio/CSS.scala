package vocaradio

import scala.util.Random
import scala.concurrent.duration._
import scalacss.DevDefaults._
import scalacss.internal.DslBase.ToStyle

object CSS extends StyleSheet.Inline {
  import dsl._

  // TODO: contribute to scalacss?
  def placeholder(styles: ToStyle*) = {
    Pseudo.Custom("::placeholder", PseudoType.Element)(styles:_*)
  }

  val randomBackground = Random.shuffle(
    Seq("sur_les_nuages", "neige", "AELIA_MARITIMA")
  ).head

  val leftPanel = style(
    position.fixed,
    top.`0`,
    left.`0`,
    width(23.%%),
    height(100.vh),
    backgroundImage := s"url('/assets/img/${randomBackground}.jpg')",
    backgroundPosition := "center",
    backgroundSize := "cover",
    media.maxWidth(700.px)(
      display.none
    )
  )

  val middlePanel = style(
    position.relative,
    overflow.auto,
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
    &.firstLetter(
      color(c"#ff5050")
    ),
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

  val separator = style(
    border.none,
    borderTop(1.px, solid, c"#eaeaea"),
    textAlign.center,
    lineHeight(1.rem),
    marginTop(1.5.rem),
    marginBottom(0.5.rem),
    overflow.visible,
    &.after(
      content := "attr(data-content)",
      display.inlineBlock,
      position.relative,
      top(-0.5.rem),
      padding(0.px, 5.px),
      backgroundColor(c"#3d3d3d")
    )
  )

  val queue = style(
    display.flex,
    flexWrap.wrap,
    justifyContent.spaceBetween,
    marginBottom(1.rem),
    overflow.hidden
  )

  val videoWidthWrapper = style(
    position.relative,
    width :=! "calc(25% - 3px)",
    marginBottom(4.px),
    media.maxWidth(700.px)(
      width :=! "calc(50% - 2px)"
    )
  )

  val videoHeightWrapper = style(
    position.relative,
    height.`0`,
    width(100.%%),
    paddingTop(65.%%)
  )

  val video = style(
    position.absolute,
    top.`0`,
    left.`0`,
    width(100.%%),
    height(100.%%),
    backgroundSize := "cover",
    backgroundPosition := "center"
  )

  val videoCover = style(
    width(100.%%),
    height(100.%%),
    backgroundColor(rgba(0, 0, 0, 0.8)),
    transition := "all .2s",
    opacity(0),
    fontSize(0.8.rem),
    &.hover(
      opacity(1)
    )
  )

  val videoLink = style(
    display.block,
    padding(5.px),
    // TODO: why can't we use a Seq here?
    &.link(color(c"#eaeaea")),
    &.visited(color(c"#eaeaea")),
    &.hover(color(c"#eaeaea")),
    &.active(color(c"#eaeaea"))
  )

  val toLeft = style(
    animationName(
      keyframes(
        100.%% -> keyframe(left :=! "calc(-100% - 4px)")
      )
    ),
    animationDuration(1.second),
    animationFillMode.forwards
  )

  val toRight = style(
    animationName(
      keyframes(
        100.%% -> keyframe(left :=! "calc(100% + 4px)")
      )
    ),
    animationDuration(1.second),
    animationFillMode.forwards
  )

  val fromLeft = style(
    animationName(
      keyframes(
        0.%% -> keyframe(left :=! "calc(-100% - 4px)"),
        100.%% -> keyframe(left(0.px))
      )
    ),
    animationDuration(1.second)
  )

  val fromRight = style(
    animationName(
      keyframes(
        0.%% -> keyframe(left :=! "calc(100% + 4px)"),
        100.%% -> keyframe(left(0.px))
      )
    ),
    animationDuration(1.second)
  )

  val portal = style(
    display.flex,
    padding(0.4.rem),
  )

  val portalName = style(
    position.relative,
    flexGrow(3),
    marginRight(0.4.rem),
  )

  val portalNameInput = style(
    width(100.%%),
    color(c"#eaeaea"),
    backgroundColor(c"#3d3d3d"),
    border.none,
    padding(0.375.rem, 0.75.rem, 0.375.rem, 0.75.rem),
    fontSize(1.rem),
    lineHeight(1.5.rem),
    outline.none,
    boxSizing.borderBox,
    resize.none,
    borderRadius(0.25.rem)
  )

  val portalBtn = style(
    flexGrow(1)
  )

  val adminControlRow = style(
    margin(0.rem, 0.2.rem, 0.rem, 1.rem)
  )

  val adminControl = style(
    display.inlineBlock,
    margin(0.rem, 0.8.rem, 1.rem, 0.rem)
  )

  val adminControlInput = style(
    color.white,
    backgroundColor(c"#666666"),
    border.none,
    padding(0.375.rem, 0.75.rem, 0.375.rem, 0.75.rem),
    fontSize(1.rem),
    lineHeight(1.5.rem),
    outline.none,
    boxSizing.borderBox,
    resize.none,
    borderRadius(0.25.rem),
    placeholder(
      color(c"#b0b0b0")
    ),
    media.maxWidth(700.px)(
      width(100.%%)
    )
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

  val tooltip = style(
    position.absolute,
    zIndex(1),
    width(120.px),
    top :=! "calc(100% + 5px)",
    left :=! "calc(50% - 60px)",
    backgroundColor(c"#ffcc00"),
    color.black,
    textAlign.center,
    padding(5.px),
    borderRadius(6.px)
  )

  val tooltipArrow = style(
    position.absolute,
    bottom(100.%%),
    left :=! "calc(50% - 5px)",
    borderWidth(5.px),
    borderStyle.solid,
    borderColor(transparent, transparent, c"#ffcc00", transparent)
  )
}
