package org.corespring.lti.web.controllers

import play.api.mvc.{ Controller, Action }

object Partials extends Controller {
  def main = Action(Ok(org.corespring.lti.web.views.html.partials.main()))
  def view = Action { request =>
    Ok(org.corespring.lti.web.views.html.partials.view())
  }
  def browse = Action(Ok(org.corespring.lti.web.views.html.partials.browse()))
}
