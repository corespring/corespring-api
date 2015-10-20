package org.corespring.v2.player.hooks

import com.mongodb.DBObject
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.container.client.hooks.Hooks.{ R, StatusMessage }
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.container.client.{ hooks => containerHooks }
import org.corespring.conversion.qti.transformers.ItemTransformer
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.models.item.{ Item => ModelItem, PlayerDefinition }
import org.corespring.services.item.ItemService
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.auth.{ ItemAuth, LoadOrgAndOptions }
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.RequestHeader

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

object V2ErrorToTuple {
  import scala.language.implicitConversions

  implicit def v2ErrorToTuple[A](v: Validation[V2Error, A]): Either[(Int, String), A] = v.leftMap { e => (e.statusCode -> e.message) }.toEither
}

class ItemHooks(
  transformer: ItemTransformer,
  auth: ItemAuth[OrgAndOpts],
  itemService: ItemService,
  getOrgAndOptsFn: RequestHeader => Validation[V2Error, OrgAndOpts],
  override implicit val containerContext: ContainerExecutionContext)
  extends containerHooks.ItemHooks
  with LoadOrgAndOptions {

  import V2ErrorToTuple._

  override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = getOrgAndOptsFn.apply(request)

  lazy val logger = Logger(classOf[ItemHooks])

  override def load(itemId: String)(implicit header: RequestHeader): Future[Either[StatusMessage, JsValue]] = Future {
    val item: Validation[V2Error, JsValue] = for {
      identity <- getOrgAndOptions(header)
      vid <- VersionedId(itemId).toSuccess(cantParseItemId(itemId))
      item <- auth.loadForRead(itemId)(identity)
    } yield transformer.transformToV2Json(item)

    item.leftMap(e => e.statusCode -> e.message).toEither
  }

  override def delete(id: String)(implicit h: RequestHeader): R[JsValue] = Future {
    for {
      identity <- getOrgAndOptions(h)
      vid <- auth.delete(id)(identity)
    } yield Json.obj("id" -> vid.toString)
  }

  private def updateDb[A](id: String, dbKey: String, data: A, returnKey: Option[String] = None)(implicit h: RequestHeader, w: Writes[A]): Either[(Int, String), JsValue] = {

    def update(vid: VersionedId[ObjectId], dbo: DBObject): Validation[V2Error, Boolean] = {
      val ok = itemService.saveUsingDbo(vid, MongoDBObject("$set" -> dbo), false)
      logger.debug(s"function=updateDb saveOk=$ok")
      if (ok) Success(ok) else Failure(generalError(s"failed to update item $id with $dbo"))
    }

    import com.mongodb.util.JSON
    val dbo = MongoDBObject(dbKey -> JSON.parse(Json.stringify((Json.toJson(data)))))

    for {
      identity <- getOrgAndOptions(h)
      vid <- VersionedId(id).toSuccess(cantParseItemId(id))
      canWrite <- auth.canWrite(id)(identity)
      _ <- if (canWrite) update(vid, dbo) else Failure(generalError(s"Can't write to this item: $id"))
    } yield {
      val outKey = returnKey.getOrElse(dbKey)
      JsObject(Seq(outKey -> w.writes(data)))
    }
  }

  override def saveXhtml(id: String, xhtml: String)(implicit h: RequestHeader): R[JsValue] = Future {
    updateDb[String](id, "playerDefinition.xhtml", xhtml, Some("xhtml"))
  }

  override def saveCollectionId(id: String, collectionId: String)(implicit h: RequestHeader): R[JsValue] = Future {
    updateDb[String](id, "collectionId", collectionId)
  }

  override def saveCustomScoring(id: String, customScoring: String)(implicit header: RequestHeader): R[JsValue] = Future {
    updateDb[String](id, "playerDefinition.customScoring", customScoring, Some("customScoring"))
  }

  override def saveSupportingMaterials(id: String, json: JsValue)(implicit h: RequestHeader): R[JsValue] = Future {
    updateDb[JsValue](id, "playerDefinition.supportingMaterials", json, Some("supportingMaterials"))
  }

  override def saveComponents(id: String, json: JsValue)(implicit h: RequestHeader): R[JsValue] = Future {
    updateDb[JsValue](id, "playerDefinition.components", json, Some("components"))
  }

  override def saveSummaryFeedback(id: String, feedback: String)(implicit h: RequestHeader): R[JsValue] = Future {
    updateDb[String](id, "playerDefinition.summaryFeedback", feedback, Some("summaryFeedback"))
  }

  override def saveProfile(id: String, json: JsValue)(implicit h: RequestHeader): R[JsValue] = Future {
    updateDb[JsValue](id, "playerDefinition.profile", json, Some("profile"))
  }

  override def createItem(maybeJson: Option[JsValue])(implicit header: RequestHeader): Future[Either[StatusMessage, String]] = Future {

    def createItem(collectionId: String, identity: OrgAndOpts): Option[VersionedId[ObjectId]] = {
      val definition = PlayerDefinition(Seq(), "<div>I'm a new item</div>", Json.obj(), "", None)
      val item = ModelItem(
        collectionId = collectionId,
        playerDefinition = Some(definition))
      auth.insert(item)(identity)
    }

    val accessResult: Validation[V2Error, VersionedId[ObjectId]] = for {
      identity <- getOrgAndOptions(header)
      json <- maybeJson.toSuccess(noJson)
      collectionId <- (json \ "collectionId").asOpt[String].toSuccess(propertyNotFoundInJson("collectionId"))
      canWrite <- auth.canCreateInCollection(collectionId)(identity)
      hasAccess <- if (canWrite) {
        Success(true)
      } else {
        Failure(generalError("Write to item denied"))
      }
      id <- createItem(collectionId, identity).toSuccess(generalError("Error creating item"))
    } yield id

    accessResult.leftMap(e => e.statusCode -> e.message).rightMap(_.toString()).toEither
  }

}

