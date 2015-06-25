package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.platform.core.models.metadata.{ Metadata, MetadataSet }
import org.corespring.platform.core.services.metadata.{ SetJson, MetadataService, MetadataSetService }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors.noToken
import play.api.libs.json.Json._
import play.api.libs.json.{ JsArray, JsValue, Json }
import play.api.mvc.{ RequestHeader, SimpleResult, Action }

import scala.concurrent.Future
import scalaz.Success

trait MetadataApi extends V2Api {

  def metadataSetService: MetadataSetService
  def metadataService: MetadataService

  implicit private def seqToJsValue(l: Seq[MetadataSet]): JsValue = JsArray(l.map(toJson(_)))
  implicit private def metadataToJsValue(m: MetadataSet): JsValue = toJson(m)

  def getByItemId(itemId: VersionedId[ObjectId]) = Action.async { implicit request =>
    Future {
      withIdentity { identity =>
        val sets = metadataSetService.list(identity.org.id)
        val metadata = metadataService.get(itemId, sets.map(_.metadataKey))
        val setAndData = sets.map(s => (s, metadata.find(_.key == s.metadataKey)))
        Ok(Json.prettyPrint(Json.toJson(setAndData.map(t => SetJson(t._1, t._2)))))
      }
    }
  }

  def get = Action.async { implicit request =>
    Future {
      withIdentity { identity => Ok(Json.prettyPrint(metadataSetService.list(identity.org.id).as[JsValue])) }
    }
  }

  private def withIdentity(block: OrgAndOpts => SimpleResult)(implicit request: RequestHeader) =
    getOrgAndOptions(request) match {
      case Success(identity) => block(identity)
      case _ => BadRequest(noToken(request).json)
    }

}
