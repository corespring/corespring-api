package org.corespring.play.utils

import play.api.mvc._
import play.api.Mode

/**
 * Dev mode support for re-loading components if the player or editor is loaded.
 */
object CallBlockOnHeaderFilter extends EssentialFilter {

  var block: (RequestHeader => Unit) = null

  def apply(next: EssentialAction) = new EssentialAction {

    def apply(request: RequestHeader) = {
      play.api.Play.current.mode match {
        case Mode.Dev => {

          if (block != null) {
            block(request)
          }

          next(request)
        }
        case _ => next(request)
      }
    }
  }
}
