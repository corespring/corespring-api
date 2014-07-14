package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.v2.auth.cookies.V2PlayerCookieReader
import org.corespring.v2.errors.Errors.identificationFailed
import org.corespring.v2.errors.V2Error
import play.api.mvc.RequestHeader

import scalaz.Scalaz._
import scalaz.Validation

trait ClientIdSessionIdentity[B] extends OrgRequestIdentity[B] with V2PlayerCookieReader {

  override def headerToOrgId(rh: RequestHeader): Validation[V2Error, ObjectId] = {
    orgIdFromCookie(rh).toSuccess(identificationFailed(rh, "clientId+opts-session")).rightMap(new ObjectId(_))
  }

  override def toString = "[ClientId-Session]"
}
