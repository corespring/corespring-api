package web.controllers

import play.api.mvc.{Action, Controller}
import web.models.QtiTemplate

object Partials extends Controller{

  def itemCollection = Action{ Ok(web.views.html.partials.itemCollection( QtiTemplate.all() )) }
  def editItem = Action{ Ok(web.views.html.partials.editItem() ) }
  def createItem = Action{ Ok(web.views.html.partials.createItem() ) }
  def home = Action{ Ok(web.views.html.partials.home() )}
  def viewItem = Action{ Ok(web.views.html.partials.viewItem() )}
}
