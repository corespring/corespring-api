package org.corespring.v2.auth

import org.corespring.models.appConfig.ArchiveConfig
import org.corespring.models.item.PlayerDefinition
import org.corespring.models.json.JsonFormatting
import org.corespring.conversion.qti.transformers.ItemTransformer
import org.corespring.services.{OrgCollectionService, OrganizationService}
import org.corespring.services.item.ItemService
import org.corespring.v2.auth.models.{OrgAndOpts, PlayerAccessSettings}
import org.corespring.v2.auth.wired.{HasPermissions, ItemAuthWired, PlayerDefinitionLoaderWired, SessionAuthWired}
import org.corespring.v2.errors.V2Error
import org.corespring.v2.sessiondb.SessionServices

import scalaz.Validation

trait V2AuthModule {

  import com.softwaremill.macwire.MacwireMacros._

  def accessSettingsCheckConfig: AccessSettingsCheckConfig
  def archiveConfig: ArchiveConfig
  def itemService: ItemService
  def itemTransformer: ItemTransformer
  def jsonFormatting: JsonFormatting
  def orgCollectionService: OrgCollectionService
  def orgService: OrganizationService

  lazy val accessSettingsWildcardCheck = new AccessSettingsWildcardCheck(accessSettingsCheckConfig)

  lazy val perms: HasPermissions = new HasPermissions {
    import org.corespring.v2.auth.models.Mode
    override def has(itemId: String, sessionId: Option[String], settings: PlayerAccessSettings): Validation[V2Error, Boolean] = {
      accessSettingsWildcardCheck.allow(itemId, sessionId, Mode.evaluate, settings)
    }
  }

  def sessionServices: SessionServices

  lazy val itemAccess: ItemAccess = wire[ItemAccess]
  lazy val itemAuth: ItemAuth[OrgAndOpts] = wire[ItemAuthWired]
  lazy val playerDefinitionLoader: PlayerDefinitionLoader = wire[PlayerDefinitionLoaderWired]
  lazy val sessionAuth: SessionAuth[OrgAndOpts, PlayerDefinition] = wire[SessionAuthWired]
}
