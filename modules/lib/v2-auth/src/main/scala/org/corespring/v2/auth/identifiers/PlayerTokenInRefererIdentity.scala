package org.corespring.v2.auth.identifiers

import play.api.mvc.RequestHeader
import play.core.parsers.FormUrlEncodedParser

object PlayerTokenInRefererIdentity {

  implicit class RequestHeaderWithRefererQueryString(rh: RequestHeader) {

    /**
     * Returns a query string in the form of a Map[String, Seq[String]] from the referer URL.
     */
    def getRefererQueryString: Map[String, Seq[String]] =
      rh.headers.get("Referer").map(referer => FormUrlEncodedParser.parse(referer.split('?').drop(1).mkString("?")))
        .getOrElse(Map.empty[String, Seq[String]])

  }

}

/**
 * A QueryStringIdentity which obtains its query string from the referer header of the request.
 */
trait PlayerTokenInRefererIdentity extends QueryStringIdentity {

  import org.corespring.v2.auth.identifiers.PlayerTokenInRefererIdentity._

  def getQueryString(rh: RequestHeader) = rh.getRefererQueryString

}

