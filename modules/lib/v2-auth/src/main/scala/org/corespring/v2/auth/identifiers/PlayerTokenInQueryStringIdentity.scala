package org.corespring.v2.auth.identifiers

import org.corespring.v2.log.V2LoggerFactory
import play.api.mvc.RequestHeader

/**
 * A QueryStringIdentity which loads the query string from the URL of the request.
 */
trait PlayerTokenInQueryStringIdentity extends QueryStringIdentity {

  override lazy val logger = V2LoggerFactory.getLogger("auth", "PlayerTokenInQueryStringIdentity")

  def getQueryString(rh: RequestHeader) = rh.queryString.map{ case(k, l) => (k, l.seq) }.toMap

}

