package player.accessControl.auth

import play.api.mvc._
import play.api.mvc.Results._

trait SafariWorkaround {

  val REDIRECT_ROUTE = "session/redirect"

  def isSafari(request: Request[AnyContent]): Boolean = {
    val userAgent = request.headers("user-agent")
    userAgent.contains("Safari") && !userAgent.contains("Chrome")
  }

  /**
   * This handles a few redirects related to the known issue with Safari: http://bit.ly/HWRRBU
   * Essentially, the code will redirect out of an iframe, set the cookies using JS, and then redirect back to the
   * original page.
   */
  def handleSafari(request: Request[AnyContent]): SimpleResult[String] = {
    request.path.endsWith(REDIRECT_ROUTE) match {
      case true => {
        val referer = request.queryString.get("referer").getOrElse(Seq[String]("")).head
          Ok(
            """<script src='/player.js?%s'></script>
               <script type="text/javascript">window.location='%s';</script>""".format(request.rawQueryString, referer)
          )
        }
      case false => {
        Ok("<script type='text/javascript'>top.location.href = 'session/redirect?%s&referer=%s';</script>"
          .format(request.rawQueryString, request.headers("Referer")))
      }
    }
  }

}
