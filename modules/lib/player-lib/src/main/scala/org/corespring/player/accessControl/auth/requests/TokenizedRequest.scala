package org.corespring.player.accessControl.auth.requests

import play.api.mvc.{ WrappedRequest, Request }

/** A wrapped request that appends an access token on to the query string */
case class TokenizedRequest[A](token: String, r: Request[A]) extends WrappedRequest(r) {
  override def queryString: Map[String, Seq[String]] = {
    super.queryString + ("access_token" -> Seq(token))
  }
}
