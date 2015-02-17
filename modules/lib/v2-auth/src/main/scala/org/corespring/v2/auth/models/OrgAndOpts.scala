package org.corespring.v2.auth.models

import org.corespring.platform.core.models.Organization
import org.corespring.v2.auth.models.AuthMode.AuthMode
import org.corespring.v2.warnings.V2Warning
import play.api.libs.json.{ JsString, Json, JsValue }

object AuthMode extends Enumeration {
  type AuthMode = Value
  val UserSession, AccessToken, ClientIdAndPlayerToken = Value
}

object IdentityJson {
  def apply(orgAndOpts: OrgAndOpts): JsValue = {
    Json.obj(
      "orgId" -> orgAndOpts.org.id.toString,
      "authMode" -> orgAndOpts.authMode.toString,
      "apiClient" -> JsString(orgAndOpts.apiClientId.getOrElse("unknown")))
  }
}

case class OrgAndOpts(
  org: Organization,
  opts: PlayerAccessSettings,
  authMode: AuthMode,
  apiClientId: Option[String],
  warnings: Seq[V2Warning] = Seq.empty)

