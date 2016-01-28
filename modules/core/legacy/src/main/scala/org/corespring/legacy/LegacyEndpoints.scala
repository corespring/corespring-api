package org.corespring.legacy

import play.api.mvc.Action
import play.mvc.Controller

object LegacyEndpoints extends Controller {

  @deprecated("remove once the docs have removed the 'encrypt-options' path", "")
  def encryptOptions = Action { request =>
    play.api.mvc.Results.MovedPermanently("/api/v2/player-token")
  }
}
