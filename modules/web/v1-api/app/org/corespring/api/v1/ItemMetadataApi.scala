package org.corespring.api.v1

import org.bson.types.ObjectId
import org.corespring.models.json.metadata.SetJson
import org.corespring.models.metadata.{ Metadata, MetadataSet }
import org.corespring.platform.core.controllers.auth.{ OAuthProvider, BaseApi }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.metadata.{ MetadataSetService, MetadataService }
import play.api.libs.json.{ Json, JsValue }

class ItemMetadataApi(
  metadataService: MetadataService,
  setService: MetadataSetService,
  val oauthProvider: OAuthProvider) extends BaseApi {

  def get(itemId: VersionedId[ObjectId]) = ApiAction {
    request =>
      val sets: Seq[MetadataSet] = setService.list(request.ctx.orgId)
      val metadata: Seq[Metadata] = metadataService.get(itemId, sets.map(_.metadataKey))
      val setAndData: Seq[(MetadataSet, Option[Metadata])] = sets.map(s => (s, metadata.find(_.key == s.metadataKey)))
      val json: Seq[JsValue] = setAndData.map(t => SetJson(t._1, t._2))
      Ok(Json.toJson(json))
  }
}

