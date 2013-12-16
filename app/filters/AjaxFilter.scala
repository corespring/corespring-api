package filters

import play.api.mvc.{ EssentialFilter, RequestHeader, EssentialAction }
import scala.concurrent.ExecutionContext

object AjaxFilter extends EssentialFilter {

  import ExecutionContext.Implicits.global

  def apply(next: EssentialAction) = new EssentialAction {

    def apply(request: RequestHeader) = {
      if (request.headers.get("X-Requested-With") == Some("XMLHttpRequest")) {
        next(request).map(result => {
          result.withHeaders(("Cache-Control", "no-cache"))
        })
      } else {
        next(request)
      }
    }

  }

}
