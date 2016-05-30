package org.corespring.v2.auth

import org.corespring.models.item.PlayerDefinition
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.V2Error
import play.api.libs.json.JsValue

import scalaz.Validation

trait PlayerDefinitionLoader {

  def loadPlayerDefinition(sessionId: String, session: JsValue)(implicit identity: OrgAndOpts): Validation[V2Error, PlayerDefinition]
  def loadMultiplePlayerDefinitions(sessions: Seq[(String, Validation[V2Error, JsValue])])(implicit identity: OrgAndOpts): Seq[(String, Validation[V2Error, (JsValue, PlayerDefinition)])]

}
