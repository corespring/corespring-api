package org.corespring.v2.player.hooks

import org.bson.types.ObjectId
import org.corespring.container.client.hooks.Hooks.{ R, StatusMessage }
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.container.client.{ hooks => containerHooks }
import org.corespring.conversion.qti.transformers.{ PlayerJsonToItem, ItemTransformer }
import org.corespring.models.json.JsonFormatting
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
  val jsonFormatting: JsonFormatting,
  val playerJsonToItem: PlayerJsonToItem,
  getOrgAndOptsFn: RequestHeader => Validation[V2Error, OrgAndOpts],
  override implicit val containerContext: ContainerExecutionContext)
  extends containerHooks.ItemHooks
  with BaseItemHooks
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

  override protected def update(id: String, json: JsValue, updateFn: (ModelItem, JsValue) => ModelItem)(implicit header: RequestHeader): R[JsValue] = Future {
    for {
      identity <- getOrgAndOptions(header)
      vid <- VersionedId(id).toSuccess(cantParseItemId(id))
      item <- auth.loadForWrite(id)(identity)
      updatedItem <- Success(updateFn(item, json))
      _ <- itemService.save(updatedItem, false).leftMap(e => generalError(e.message))
    } yield json
  }

}

