package api.v1

import controllers.auth.BaseApi
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json.{Json, JsValue}
import org.corespring.platform.core.services.metadata._
import org.corespring.platform.core.models.metadata.{Metadata, MetadataSet}
import org.corespring.platform.core.services.item.{ItemService, ItemServiceImpl, ItemServiceClient}
import org.corespring.platform.core.models.metadata.Metadata
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.core.models.Organization


class ItemMetadataApi(metadataService: MetadataService, setService: MetadataSetService) extends BaseApi {

  def get(id: VersionedId[ObjectId]) = ApiAction {
    request =>
      val sets: Seq[MetadataSet] = setService.list(request.ctx.organization)
      val metadata: Seq[Metadata] = metadataService.get(id, sets.map(_.metadataKey))
      val setAndData: Seq[(MetadataSet, Option[Metadata])] = sets.map(s => (s, metadata.find(_.key == s.metadataKey)))
      val json: Seq[JsValue] = setAndData.map(t => SetJson(t._1, t._2))
      Ok(Json.toJson(json))
  }
}

object ItemMetadataApi extends ItemMetadataApi(
  new MetadataServiceImpl with ItemServiceClient {
    def itemService: ItemService = ItemServiceImpl
  },
  new MetadataSetServiceImpl {
    def orgService: OrganizationService = Organization
  }
)


