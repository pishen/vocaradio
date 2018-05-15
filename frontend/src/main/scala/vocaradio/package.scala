import org.scalajs.dom.raw._
import scala.scalajs.js

package object vocaradio {
  implicit class RichHTMLElement(h: HTMLElement) {
    def hide() = h.asInstanceOf[js.Dynamic].hidden = true
    def show() = h.asInstanceOf[js.Dynamic].hidden = false
    def childrenSeq = js.Array
      .asInstanceOf[js.Dynamic]
      .from(h.children)
      .asInstanceOf[js.Array[HTMLElement]]
      .toSeq
  }
}
