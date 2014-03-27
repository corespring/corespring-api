package org.corespring.player.accessControl.auth

import play.api.mvc._
import play.api.mvc.Results._
import play.mvc.Http.HeaderNames._
import scala.concurrent._
import ExecutionContext.Implicits.global

trait SafariWorkaround {

  val REDIRECT_ROUTE = "session/redirect"

  def isSafari(request: Request[AnyContent]): Boolean = {
    val userAgent = request.headers(USER_AGENT)
    userAgent.contains("Safari") && !userAgent.contains("Chrome")
  }

  /**
   * This handles a few redirects related to the known issue with Safari: http://bit.ly/HWRRBU
   * Essentially, the code will redirect out of an iframe, set the cookies using JS, and then redirect back to the
   * original page.
   */
  def handleSafari(request: Request[AnyContent]): Future[SimpleResult] = Future({
    request.path.endsWith(REDIRECT_ROUTE) match {
      case true => {
        val referer = request.queryString.get(REFERER).getOrElse(Seq[String]("")).head
        Ok(s"""<head><script src='/player.js?${request.rawQueryString}'></script>
               <script type="text/javascript">window.location='${referer}';</script></head>""")
      }
      case _ => {
        Ok("<script type='text/javascript'>top.location.href = 'session/redirect?%s&referer=%s';</script>"
          .format(request.rawQueryString, request.headers(REFERER)))
      }
    }
  })

}