package org.corespring.v2.auth

import org.corespring.common.config.AppConfig
import org.corespring.models.item.PlayerDefinition
import org.corespring.models.json.JsonFormatting
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.services.OrganizationService
import org.corespring.services.item.ItemService
import org.corespring.v2.auth.models.{ PlayerAccessSettings, OrgAndOpts }
import org.corespring.v2.auth.wired.{ HasPermissions, ItemAuthWired, SessionAuthWired }
import org.corespring.v2.errors.V2Error
import org.corespring.v2.sessiondb.SessionServices

import scalaz.Validation

trait V2AuthModule {

  import com.softwaremill.macwire.MacwireMacros._

  def appConfig: AppConfig
  def jsonFormatting: JsonFormatting
  def itemService: ItemService
  def orgService: OrganizationService
  def itemTransformer: ItemTransformer

  lazy val accessSettingsWildcardCheck = new AccessSettingsWildcardCheck(AccessSettingsCheckConfig(appConfig.allowAllSessions))

  lazy val perms: HasPermissions = new HasPermissions {
    import org.corespring.v2.auth.models.Mode
    override def has(itemId: String, sessionId: Option[String], settings: PlayerAccessSettings): Validation[V2Error, Boolean] = {
      accessSettingsWildcardCheck.allow(itemId, sessionId, Mode.evaluate, settings)
    }
  }

  def sessionServices: SessionServices

  lazy val itemAccess: ItemAccess = wire[ItemAccess]
  lazy val itemAuth: ItemAuth[OrgAndOpts] = wire[ItemAuthWired]
  lazy val sessionAuth: SessionAuth[OrgAndOpts, PlayerDefinition] = wire[SessionAuthWired]
}
