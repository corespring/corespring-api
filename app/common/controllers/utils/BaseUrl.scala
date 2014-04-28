package common.controllers.utils

import play.api.mvc.Request
import play.api.mvc.AnyContent

object BaseUrl {
  def apply(r: Request[AnyContent]): String = {

    /**
     * Note: You can't check a request to see if its http or not in Play
     * But even if you could you may be sitting behind a reverse proxy.
     * @see: https://groups.google.com/forum/?fromgroups=#!searchin/play-framework/https$20request/play-framework/11zbMtNI3A8/o4318Z-Ir6UJ
     * but the tip was to check for the header below
     */
    val protocol = r.headers.get("x-forwarded-proto") match {
      case Some("https") => "https"
      case _ => "http"
    }

    protocol + "://" + r.host
  }
}
