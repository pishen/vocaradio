package vocaradio

import scalacss.DevDefaults._

object IndexStyles extends StyleSheet.Inline {
  import dsl._

  val notoSansTC = fontFace(
    _.src(
      "url(//fonts.gstatic.com/ea/notosanstc/v1/NotoSansTC-Regular.woff2) format('woff2')",
      "url(//fonts.gstatic.com/ea/notosanstc/v1/NotoSansTC-Regular.woff) format('woff')",
      "url(//fonts.gstatic.com/ea/notosanstc/v1/NotoSansTC-Regular.otf) format('opentype')"
    )
  )

  val logoFont = fontFace(
    _.src(
      "local('Chelsea Market')",
      "local('ChelseaMarket-Regular')",
      "url(https://fonts.gstatic.com/s/chelseamarket/v4/qSdzwh2A4BbNemy78sJLfGumFyKY0eCdXHjD5427TIo.woff2) format('woff2')"
    )
  )

  val html = style(
    minHeight(100.vh)
  )

  val body = style(
    fontFamily(notoSansTC),
    minHeight(100.vh),
    margin(0.px),
    color(c"#eaeaea")
  )

  val leftPanel = style(
    position.fixed,
    top.`0`,
    left.`0`,
    width(23.%%),
    height(100.vh),
    backgroundImage := "url('/assets/img/neige.jpg')",
    backgroundPosition := "center",
    backgroundSize := "cover"
  )

  val middlePanel = style(
    position.relative,
    left(23.%%),
    width(54.%%),
    minHeight(100.vh),
    backgroundColor(c"#3d3d3d"),
    boxShadow := "0px 0px 10px black"
  )

  val logo = style(
    textAlign.center,
    fontSize(55.px),
    padding(15.px, 0.px, 15.px, 0.px),
    fontFamily(logoFont)
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

  // val playlistW1 = style(
  //   width(210.px),
  //   margin(2.px),
  //   flexGrow(1)
  // )

  // val playlistW2 = style(
  //   position.relative,
  //   height(0.px),
  //   overflow.hidden,
  //   width(100.%%),
  //   paddingTop(56.25.%%)
  // )

  // val playlistItem = style(
  //   position.absolute,
  //   top.`0`,
  //   left.`0`,
  //   border.`0`,
  //   width(100.%%),
  //   height(100.%%)
  // )

  val rightPanel = style(
    position.fixed,
    top.`0`,
    right.`0`,
    width(23.%%),
    height(100.vh),
    backgroundColor(c"#1e1e1e")
  )
}
