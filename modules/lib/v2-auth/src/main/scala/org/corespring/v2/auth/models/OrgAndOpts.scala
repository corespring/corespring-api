package org.corespring.v2.auth.models

import org.corespring.models.{DisplayConfig, Organization, User}
import org.corespring.v2.auth.models.AuthMode.AuthMode
import org.corespring.v2.warnings.V2Warning
import play.api.libs.json.{JsObject, JsString, JsValue, Json}

object AuthMode {
  sealed trait AuthMode { val id: Int }
  case object UserSession extends AuthMode { val id = 0 }
  case object AccessToken extends AuthMode { val id = 1 }
  case object ClientIdAndPlayerToken extends AuthMode { val id = 2 }
}

object IdentityJson {
  def apply(orgAndOpts: OrgAndOpts): JsValue = {
    Json.obj(
      "orgId" -> orgAndOpts.org.id.toString,
      "authMode" -> orgAndOpts.authMode.id,
      "apiClient" -> JsString(orgAndOpts.apiClientId.getOrElse("unknown")))
  }
}

object DisplayConfigJson {
  def apply(orgAndOpts: OrgAndOpts): JsValue = {
    implicit val displayConfigWrites = DisplayConfig.Writes
    Json.toJson(orgAndOpts.org.displayConfig).as[JsObject]
  }
}

case class OrgAndOpts(
  org: Organization,
  opts: PlayerAccessSettings,
  authMode: AuthMode,
  apiClientId: Option[String],
  user: Option[User] = None,
  warnings: Seq[V2Warning] = Seq.empty)

