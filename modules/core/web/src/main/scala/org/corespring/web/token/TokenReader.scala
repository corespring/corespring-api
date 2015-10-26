package org.corespring.web.token

import play.api.mvc.RequestHeader

object TokenReader {
  val AuthorizationHeader = "Authorization"
  val AccessToken = "access_token"
  val Bearer = "Bearer"
  val Space = " "
}

trait TokenReader {

  import TokenReader._

  /**
   * get the access token the the query string, the session or the Authorization header.
   * @param request
   * @param invalidToken
   * @param noToken
   * @tparam E
   */
  def getToken[E](request: RequestHeader, invalidToken: E, noToken: E): Either[E, String] = {

    def tokenInHeader: Option[Either[E, String]] = {
      request.headers.get(AuthorizationHeader).map { h =>
        val split = h.split(Space).toSeq

        split match {
          case Seq(Bearer, token) => Right(token)
          case _ => Left(invalidToken)
        }
      }
    }

    val queryToken: Unit => Option[String] = _ => request.queryString.get(AccessToken).map(_.head)
    val sessionToken: Unit => Option[String] = _ => request.session.get(AccessToken)
    val headerToken: Unit => Option[Either[E, String]] = _ => tokenInHeader

    val result = queryToken() orElse sessionToken() orElse headerToken()

    result match {
      case Some(Left(e)) => Left(e.asInstanceOf[E])
      case Some(Right(s)) => Right(s.asInstanceOf[String])
      case Some(s: String) => Right(s)
      case _ => Left(noToken)
    }
  }
}
