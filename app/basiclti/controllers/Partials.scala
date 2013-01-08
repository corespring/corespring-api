package basiclti.controllers

import play.api.mvc.{Controller, Action}

object Partials extends Controller{
  def main = Action(Ok(basiclti.views.html.partials.main()))
  def view = Action(Ok(basiclti.views.html.partials.view()))
  def browse = Action(Ok(basiclti.views.html.partials.browse()))
}
