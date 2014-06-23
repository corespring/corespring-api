package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.v2.auth.cookies.V2PlayerCookieReader
import play.api.mvc.RequestHeader

import scalaz.Scalaz._
import scalaz.Validation

trait ClientIdSessionIdentity[B] extends OrgRequestIdentity[B] with V2PlayerCookieReader {

  override def headerToOrgId(rh: RequestHeader): Validation[String, ObjectId] = {
    orgIdFromCookie(rh).toSuccess("Can't find org id").rightMap(new ObjectId(_))
  }
}
