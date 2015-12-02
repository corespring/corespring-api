package org.corespring.v2.player.hooks

import org.bson.types.ObjectId
import org.corespring.models.json.JsonFormatting
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors.cantParseItemId
import org.corespring.v2.errors.V2Error
import org.corespring.v2.player.V2PlayerExecutionContext
import org.corespring.v2.player.services.item.ItemSupportingMaterialsService
import play.api.mvc.RequestHeader

import scalaz.Validation

class ItemSupportingMaterialHooks(auth: ItemAuth[OrgAndOpts],
  jsonFormatting: JsonFormatting,
  getOrgAndOptsFn: RequestHeader => Validation[V2Error, OrgAndOpts],
  override val service: ItemSupportingMaterialsService,
  ec: V2PlayerExecutionContext)
  extends SupportingMaterialHooks[VersionedId[ObjectId]](auth, getOrgAndOptsFn, jsonFormatting, ec)
  with org.corespring.container.client.hooks.ItemSupportingMaterialHooks {

  import scalaz.Scalaz._

  override def parseId(id: String, identity: OrgAndOpts): Validation[V2Error, VersionedId[ObjectId]] = {
    VersionedId(id).toSuccess(cantParseItemId(id))
  }

  //    override def service: SupportingMaterialsService[VersionedId[ObjectId]] = new ItemSupportingMaterialsService(
  //      ItemServiceWired.collection,
  //      V2PlayerBootstrap.this.bucket,
  //      new SupportingMaterialsAssets(s3, bucket, ItemAssetKeys))(mongoContext.context)

  //override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = identifier(request)
  //}
}
