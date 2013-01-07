package common.controllers.utils

import play.api.mvc.Request
import play.api.mvc.AnyContent


object BaseUrl{
  def apply(r:Request[AnyContent]) : String = {

    val protocol = r.headers.get("x-forwarded-proto") match {
      case Some("https") => "https"
      case _ => "http"
    }

    protocol + "://" + r.host
  }
}
