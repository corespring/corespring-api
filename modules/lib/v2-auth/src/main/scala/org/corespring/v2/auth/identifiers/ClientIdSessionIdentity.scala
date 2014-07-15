package org.corespring.v2.auth.identifiers

import org.bson.types.ObjectId
import org.corespring.v2.auth.cookies.V2PlayerCookieReader
import org.corespring.v2.errors.V2Error
import org.corespring.v2.errors.Errors.noClientIdAndOptionsInSession
import play.api.mvc.RequestHeader
import scalaz.Validation
import scalaz.Scalaz._

trait ClientIdSessionIdentity[B] extends OrgRequestIdentity[B] with V2PlayerCookieReader {

  override def headerToOrgId(rh: RequestHeader): Validation[V2Error, ObjectId] = {
    orgIdFromCookie(rh).toSuccess(noClientIdAndOptionsInSession(rh)).rightMap(new ObjectId(_))
  }
}
