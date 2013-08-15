package api.v1

import controllers.auth.BaseApi
import models.item.Metadata
import models.item.service.{ItemService, ItemServiceClient, ItemServiceImpl}
import models.metadata._
import models.{Organization, OrganizationService, MetadataSet}
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json.{Json, JsValue}

case class SetView(set: MetadataSet, data: Metadata)


class ItemMetadataApi(metadataService: MetadataService, setService: MetadataSetService) extends BaseApi {


  /*"""
    |[
    | {
    |   "set" : {
    |     "setId" : "asdfasdf",
    |     "editorLabel" : "Label",
    |     "metadataKey" : "keyOne",
    |     "editorUrl" : "blah.org",
    |     "schema" : []
    |   },
    |   "data" : [
    |     { "key" : "Apple", "value" : "" },
    |     { "key" : "Car", "value" : "Audi" },
    |     { "key" : "Tennis", "value" : "Federer" },
    |   ]
    | }
    |]
  """.stripMargin

  Ok("") */

  def get(id: VersionedId[ObjectId]) = ApiAction {
    request =>
      val sets: Seq[MetadataSet] = setService.list(request.ctx.organization)
      val metadata: Seq[Metadata] = metadataService.get(id, sets.map(_.metadataKey))
      val setAndData: Seq[(MetadataSet, Option[Metadata])] = sets.map(s => (s, metadata.find(_.metadataKey == s.metadataKey)))
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


