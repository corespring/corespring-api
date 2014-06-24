package org.corespring.dev.tools.controllers

import org.corespring.dev.tools.DevTools
import play.api.mvc.{AnyContent, Action, SimpleResult, Request}

object DevToolsAction{

  def apply[A >: AnyContent](block: Request[A] => SimpleResult) = Action{ request =>
    if(DevTools.enabled){
      block(request)
    } else {
      play.api.mvc.Results.NotFound("")
    }
  }
}
