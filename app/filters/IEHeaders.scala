package filters

import play.api.mvc.{ EssentialFilter, RequestHeader, EssentialAction }

object IEHeaders extends EssentialFilter {

  /**
   * Note: For Any content hosted in an iframe to support IE we need to add some p3p tags
   * see: http://stackoverflow.com/questions/389456/cookie-blocked-not-saved-in-iframe-in-internet-explorer
   */
  val p3pHeaders = ("P3P", """CP="NOI ADM DEV COM NAV OUR STP"""")

  def apply(next: EssentialAction) = new EssentialAction {

    def apply(request: RequestHeader) = {
      next(request).map(result => result.withHeaders(p3pHeaders))
    }
  }
}
