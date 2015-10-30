package controllers

import better.files._

object CSSHelper {
  def generatePos(outputPath: String) = {
    val numOfColumns = 5
    val lines = (0 until 25).map { i =>
      val xi = i % numOfColumns
      val yi = i / numOfColumns
      val x = (if (yi % 2 == 1) numOfColumns - 1 - xi else xi) * (220 + 10)
      val y = yi * (124 + 10)
      s".pos${i} {left:${x}px;top:${y}px}"
    }
    File(outputPath).overwrite("").appendLines(lines: _*)
  }
}
