package filters

import play.api.mvc.{EssentialFilter, SimpleResult, RequestHeader, EssentialAction}

object AjaxFilter extends EssentialFilter {

  def apply(next: EssentialAction) = new EssentialAction {

    def apply(request: RequestHeader) = {
      if (request.headers.get("X-Requested-With") == Some("XMLHttpRequest")) {
        next(request).map(result => result.withHeaders(("Cache-Control", "no-cache")))
      } else {
        next(request)
      }
    }

  }

}
