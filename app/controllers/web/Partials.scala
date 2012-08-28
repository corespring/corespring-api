package controllers.web

import play.api.mvc.{Action, Controller}
import models.web.QtiTemplate

object Partials extends Controller{

  def itemCollection = Action{ Ok(views.html.web.partials.itemCollection( QtiTemplate.all() )) }
  def editMetadata = Action{ Ok(views.html.web.partials.editMetadata() ) }
  def createItem = Action{ Ok(views.html.web.partials.createItem() ) }
}
