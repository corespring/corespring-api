package org.corespring.v2.player.hooks

import org.corespring.drafts.item.models.DraftId
import org.corespring.platform.core.services.item.SupportingMaterialsService
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.V2Error
import play.api.mvc.RequestHeader

import scalaz.Validation

trait ItemDraftSupportingMaterialHooks extends SupportingMaterialHooks[DraftId] {
  override def auth: ItemAuth[OrgAndOpts] = ???

  override def parseId(id: String): Validation[V2Error, DraftId] = ???

  override def service: SupportingMaterialsService[DraftId] = ???

  override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = ???
}
