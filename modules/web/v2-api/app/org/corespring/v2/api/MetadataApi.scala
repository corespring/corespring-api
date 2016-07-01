package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.models.json.metadata.{ MetadataSetFormat, SetJson }
import org.corespring.models.metadata.MetadataSet
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.metadata.{ MetadataService, MetadataSetService }
import org.corespring.v2.actions.{ OrgRequest, V2Actions }
import org.corespring.v2.errors.Errors._
import play.api.libs.json.Json._
import play.api.libs.json.{ Format, JsArray, JsValue, Json }
import play.api.mvc._

import scala.concurrent.ExecutionContext

class MetadataApi(
  actions: V2Actions,
  metadataSetService: MetadataSetService,
  metadataService: MetadataService,
  v2ApiContext: V2ApiExecutionContext) extends V2Api {

  override implicit def ec: ExecutionContext = v2ApiContext.context

  implicit val ms: Format[MetadataSet] = MetadataSetFormat

  import scala.language.implicitConversions

  implicit private def seqToJsValue(l: Seq[MetadataSet]): JsValue = JsArray(l.map(toJson(_)))
  implicit private def metadataToJsValue(m: MetadataSet): JsValue = toJson(m)

  def getByItemId(itemId: VersionedId[ObjectId]) = actions.Org { request =>
    val sets = metadataSetService.list(request.org.id)
    val metadata = metadataService.get(itemId, sets.map(_.metadataKey))
    val setAndData = sets.map(s => (s, metadata.find(_.key == s.metadataKey)))
    Ok(Json.toJson(setAndData.map(t => SetJson(t._1, t._2))))
  }

  def get = actions.Org { request =>
    Ok(metadataSetService.list(request.org.id).as[JsValue])
  }

  def create() = actions.Org { request =>
    val json = request.body.asJson.getOrElse(Json.obj())
    (try {
      json.asOpt[MetadataSet]
    } catch {
      case e: Exception => None
    }) match {
      case Some(metadataSet) =>
        metadataSetService.create(request.org.id, metadataSet)
          .fold(e => errorSaving(e).toResult, set => Ok(Json.toJson(set)))
      case _ => incorrectJsonFormat(json).toResult
    }
  }

  def update(metadataSetId: ObjectId) = withMetadataSet(metadataSetId, { (metadataSet, request) =>
    val json = request.body.asJson.getOrElse(Json.obj())
    (try {
      json.asOpt[MetadataSet].map(_.copy(id = metadataSetId))
    } catch {
      case e: Exception => None
    }) match {
      case Some(metadataSet) =>
        metadataSetService.update(metadataSet)
          .fold(e => errorSaving(e).toResult, set => Ok(Json.toJson(set)))
      case _ => incorrectJsonFormat(json).toResult
    }
  })

  def getById(metadataSetId: ObjectId) = withMetadataSet(metadataSetId, { (metadataSet, request) =>
    Ok(Json.toJson(metadataSet))
  })

  def delete(metadataSetId: ObjectId) = withMetadataSet(metadataSetId, { (metadataSet, request) =>
    metadataSetService.delete(request.org.id, metadataSetId) match {
      case None => Ok(Json.toJson(metadataSet))
      case _ => cantFindMetadataSetWithId(metadataSetId).toResult
    }
  })

  private def withMetadataSet(metadataSetId: ObjectId, block: ((MetadataSet, OrgRequest[AnyContent]) => SimpleResult)) =
    actions.Org { request =>
      metadataSetService.findOneById(metadataSetId) match {
        case Some(metadataSet) => block(metadataSet, request)
        case _ => cantFindMetadataSetWithId(metadataSetId).toResult
      }
    }

}
