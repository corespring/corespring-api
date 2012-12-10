package common.controllers.utils

import play.api.mvc.Request
import play.api.mvc.AnyContent


object BaseUrl{
  def apply(r:Request[AnyContent]) : String = {
    val protocol = if(r.uri.startsWith("https")) "https" else "http"
    protocol + "://" + r.host
  }
}
