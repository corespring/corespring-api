package org.corespring.v2.auth.models

import org.corespring.platform.core.models.Organization
import org.corespring.v2.auth.models.AuthMode.AuthMode
import org.corespring.v2.warnings.V2Warning

object AuthMode extends Enumeration {
  type AuthMode = Value
  val UserSession, AccessToken, ClientIdAndPlayerToken = Value
}

case class OrgAndOpts(
  org: Organization,
  opts: PlayerAccessSettings,
  authMode: AuthMode,
  warnings: Seq[V2Warning] = Seq.empty)

