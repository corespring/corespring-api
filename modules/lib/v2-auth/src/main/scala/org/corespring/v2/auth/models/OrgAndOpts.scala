package org.corespring.v2.auth.models

import org.bson.types.ObjectId
import org.corespring.platform.core.models.Organization
import org.corespring.v2.auth.models.AuthMode.AuthMode
import org.corespring.v2.warnings.V2Warning

object AuthMode extends Enumeration {
  type AuthMode = Value
  val UserSession, AccessToken, ClientIdAndPlayerToken = Value
}

case class OrgAndOpts(
  orgId: ObjectId,
  opts: PlayerAccessSettings,
  authMode: AuthMode,
  org: Option[Organization] = None,
  warnings: Seq[V2Warning] = Seq.empty)

