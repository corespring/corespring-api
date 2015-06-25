package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.platform.core.models.metadata.{ Metadata, MetadataSet }
import org.corespring.platform.core.services.metadata.{ SetJson, MetadataService, MetadataSetService }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.errors.Errors.noToken
import play.api.libs.json.Json
import play.api.mvc.Action

import scala.concurrent.Future
import scalaz.Success

trait MetadataApi extends V2Api {

  def metadataSetService: MetadataSetService
  def metadataService: MetadataService

  def get(itemId: VersionedId[ObjectId]) = Action.async { implicit request =>
    Future {
      getOrgAndOptions(request) match {
        case Success(identity) => {
          val sets = metadataSetService.list(identity.org.id)
          val metadata = metadataService.get(itemId, sets.map(_.metadataKey))
          val setAndData = sets.map(s => (s, metadata.find(_.key == s.metadataKey)))
          Ok(Json.prettyPrint(Json.toJson(setAndData.map(t => SetJson(t._1, t._2)))))
        }
        case _ => BadRequest(noToken(request).json)
      }
    }
  }

}
