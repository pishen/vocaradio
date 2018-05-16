package vocaradio

import org.scalajs.dom._
import scala.scalajs.js
import scalatags.JsDom.all._
import scalacss.ScalatagsCss._

object Portal {
  val portalNameInput = input(
    CSS.portalNameInput,
    value := Option(window.localStorage.getItem("username")).getOrElse("")
  ).render

  def getUserName(): Option[String] = {
    Some(portalNameInput.value).filter(_ != "")
  }

  val portalNameTooltip = div(
    CSS.tooltip,
    div(CSS.tooltipArrow),
    "請輸入暱稱"
  ).render

  portalNameInput.onchange = { (e: Event) =>
    getUserName() match {
      case Some(name) =>
        window.localStorage.setItem("username", name)
        portalNameTooltip.hide()
      case None =>
        window.localStorage.removeItem("username")
        portalNameTooltip.show()
    }
  }
  portalNameInput.asInstanceOf[js.Dynamic].onchange()

  val portalNameDiv = div(
    CSS.portalName,
    portalNameInput,
    portalNameTooltip
  ).render
  portalNameDiv.hide()

  val portalBtn = a(
    CSS.btn,
    CSS.portalBtn,
    href := "/login", "登入"
  ).render

  def element = div(CSS.portal, portalNameDiv, portalBtn).render

  def login() = {
    portalNameDiv.show()
    portalBtn.setAttribute("href", "/logout")
    portalBtn.textContent = "登出"
  }
}
