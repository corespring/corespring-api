package org.corespring.v2.player.hooks

import org.corespring.drafts.item.MakeDraftId
import org.corespring.drafts.item.models.{DraftId, OrgAndUser, SimpleOrg, SimpleUser}
import org.corespring.models.appConfig.Bucket
import org.corespring.models.json.JsonFormatting
import org.corespring.services.item.SupportingMaterialsService
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.errors.V2Error
import org.corespring.v2.player.V2PlayerExecutionContext
import play.api.mvc.RequestHeader

import scalaz.Validation

class ItemDraftSupportingMaterialHooks( auth: ItemAuth[OrgAndOpts],
                                        bucket : Bucket,
                                        jsonFormatting:JsonFormatting,
                                        getOrgAndOptsFn : RequestHeader => Validation[V2Error,OrgAndOpts],
                                        override val service : SupportingMaterialsService[DraftId],
                                        ec: V2PlayerExecutionContext
                                      ) extends SupportingMaterialHooks[DraftId](
auth, getOrgAndOptsFn, jsonFormatting, ec) with MakeDraftId{

    override def parseId(id: String, identity: OrgAndOpts): Validation[V2Error, DraftId] = {
      val org = SimpleOrg.fromOrganization(identity.org)
      val user = identity.user.map(SimpleUser.fromUser)
      val orgAndUser = OrgAndUser(org, user)
      mkDraftId(orgAndUser, id).leftMap(e => generalError(e.msg))
    }
//
//    override def auth: ItemAuth[OrgAndOpts] = V2PlayerBootstrap.this.itemAuth
//
//    override def service: SupportingMaterialsService[DraftId] = new DraftSupportingMaterialsService(
//      itemDrafts.collection,
//      V2PlayerBootstrap.this.bucket,
//      new SupportingMaterialsAssets(s3, bucket, DraftAssetKeys))(mongoContext.context)
//
//    override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = identifier(request)
//  }

}
