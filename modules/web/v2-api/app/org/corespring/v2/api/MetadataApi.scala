package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.platform.core.models.metadata.{ Metadata, MetadataSet }
import org.corespring.platform.core.services.metadata.{ SetJson, MetadataService, MetadataSetService }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors.{ incorrectJsonFormat, errorSaving, cantFindMetadataSetWithId, noToken }
import org.corespring.v2.errors.V2Error
import play.api.libs.json.Json._
import play.api.libs.json.{ JsArray, JsValue, Json }
import play.api.mvc._

import scala.concurrent.Future
import scalaz.Success

trait MetadataApi extends V2Api {

  def metadataSetService: MetadataSetService
  def metadataService: MetadataService

  implicit private def seqToJsValue(l: Seq[MetadataSet]): JsValue = JsArray(l.map(toJson(_)))
  implicit private def metadataToJsValue(m: MetadataSet): JsValue = toJson(m)

  def getByItemId(itemId: VersionedId[ObjectId]) = withIdentity { (identity, _) =>
    val sets = metadataSetService.list(identity.org.id)
    val metadata = metadataService.get(itemId, sets.map(_.metadataKey))
    val setAndData = sets.map(s => (s, metadata.find(_.key == s.metadataKey)))
    Ok(Json.prettyPrint(Json.toJson(setAndData.map(t => SetJson(t._1, t._2)))))
  }

  def get = withIdentity { (identity, _) =>
    Ok(Json.prettyPrint(metadataSetService.list(identity.org.id).as[JsValue]))
  }

  def create() = withIdentity { (identity, request) =>
    val json = request.body.asJson.getOrElse(Json.obj())
    json.asOpt[MetadataSet] match {
      case Some(metadataSet) => metadataSetService.create(identity.org.id, metadataSet) match {
        case Left(error) => errorSaving(error).toResult
        case Right(set) => Ok(Json.prettyPrint(set))
      }
      case _ => incorrectJsonFormat(json).toResult
    }
  }

  def update(metadataSetId: ObjectId) = withIdentity { (identity, request) =>
    val json = request.body.asJson.getOrElse(Json.obj())
    json.asOpt[MetadataSet] match {
      case Some(metadataSet) => metadataSetService.update(metadataSet) match {
        case Left(error) => errorSaving(error).toResult
        case Right(set) => Ok(Json.prettyPrint(set))
      }
      case _ => incorrectJsonFormat(json).toResult
    }
  }

  def getById(metadataSetId: ObjectId) = withIdentity { (identity, _) =>
    metadataSetService.findOneById(metadataSetId) match {
      case Some(metadataSet) => Ok(Json.prettyPrint(metadataSet))
      case _ => cantFindMetadataSetWithId(metadataSetId).toResult
    }
  }

  def delete(metadataSetId: ObjectId) = withIdentity { (identity, _) =>
    metadataSetService.findOneById(metadataSetId) match {
      case Some(metadataSet) => metadataSetService.delete(identity.org.id, metadataSetId) match {
        case None => Ok(Json.prettyPrint(metadataSet))
        case _ => cantFindMetadataSetWithId(metadataSetId).toResult
      }
      case _ => cantFindMetadataSetWithId(metadataSetId).toResult
    }
  }

}
