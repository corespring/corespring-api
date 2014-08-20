package org.corespring.v2.auth.models

import org.bson.types.ObjectId
import org.corespring.platform.core.models.Organization
import org.corespring.v2.auth.models.AuthMode.AuthMode

object AuthMode extends Enumeration {
  type AuthMode = Value
  val UserSession, AccessToken, ClientIdAndOpts = Value
}

case class OrgAndOpts(orgId: ObjectId, opts: PlayerOptions, authMode: AuthMode, org: Option[Organization] = None)

