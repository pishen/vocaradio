package controllers

import javax.inject.Inject
import play.api.mvc._
import play.api.libs.ws._

class Application @Inject() (implicit ws: WSClient) extends Controller {
  def index() = Action {
    Ok(views.html.index())
  }
  
  def sync = Action {
    Ok
  }
}
