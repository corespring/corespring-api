package org.corespring.v2.auth

import org.corespring.models.item.PlayerDefinition
import org.corespring.models.json.JsonFormatting
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.services.OrganizationService
import org.corespring.services.item.ItemService
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.auth.wired.{ SessionServices, HasPermissions, ItemAuthWired, SessionAuthWired }

trait V2AuthModule {

  import com.softwaremill.macwire.MacwireMacros._

  def jsonFormatting: JsonFormatting
  def itemService: ItemService
  def orgService: OrganizationService
  def itemTransformer: ItemTransformer
  def perms: HasPermissions
  def sessionServices: SessionServices

  lazy val itemAccess: ItemAccess = wire[ItemAccess]
  lazy val itemAuth: ItemAuth[OrgAndOpts] = wire[ItemAuthWired]
  lazy val sessionAuth: SessionAuth[OrgAndOpts, PlayerDefinition] = wire[SessionAuthWired]
}
