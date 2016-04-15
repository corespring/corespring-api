package org.corespring.v2.player.hooks

import org.bson.types.ObjectId
import org.corespring.container.client.hooks.Hooks.{ R, StatusMessage }
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.container.client.{ hooks => containerHooks }
import org.corespring.conversion.qti.transformers.{ ItemTransformer, PlayerJsonToItem }
import org.corespring.models.item.{ PlayerDefinition, Item => ModelItem }
import org.corespring.models.json.JsonFormatting
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.OrgCollectionService
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
  orgCollectionService: OrgCollectionService,
  override implicit val containerContext: ContainerExecutionContext)
  extends containerHooks.ItemHooks
  with BaseItemHooks
  with LoadOrgAndOptions {

  import V2ErrorToTuple._

  override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = getOrgAndOptsFn.apply(request)

  lazy val logger = Logger(classOf[ItemHooks])

  override def load(itemId: String)(implicit header: RequestHeader): Future[Either[StatusMessage, (JsValue, JsValue)]] = Future {
    val item: Validation[V2Error, (JsValue, JsValue)] = for {
      identity <- getOrgAndOptions(header)
      vid <- VersionedId(itemId).toSuccess(cantParseItemId(itemId))
      item <- auth.loadForRead(itemId)(identity)
    } yield (transformer.transformToV2Json(item), Json.obj())

    item.leftMap(e => e.statusCode -> e.message).toEither
  }

  override def delete(id: String)(implicit h: RequestHeader): R[JsValue] = Future {
    for {
      identity <- getOrgAndOptions(h)
      vid <- auth.delete(id)(identity)
    } yield Json.obj("id" -> vid.toString)
  }

  override def createSingleComponentItem(collectionId: Option[String], componentType: String, key: String, defaultData: JsObject)(implicit h: RequestHeader): R[String] = {
    createItem(h, collectionId) { (collectionId, orgAndOpts) =>
      val xhtml = s"""<div><div $componentType="" id="$key"></div></div>"""
      val definition = PlayerDefinition(xhtml = xhtml, components = Json.obj(key -> defaultData))
      val item = ModelItem(
        collectionId = collectionId,
        playerDefinition = Some(definition))
      auth.insert(item)(orgAndOpts)
    }
  }

  override def createItem(collectionId: Option[String])(implicit header: RequestHeader): R[String] = {
    createItem(header, collectionId) { (collectionId, orgAndOpts) =>
      val definition = PlayerDefinition(Seq(), "<div>I'm a new item</div>", Json.obj(), "", None)
      val item = ModelItem(
        collectionId = collectionId,
        playerDefinition = Some(definition))
      auth.insert(item)(orgAndOpts)
    }
  }

  private def createItem(header: RequestHeader, collectionId: Option[String])(mkItem: (String, OrgAndOpts) => Option[VersionedId[ObjectId]]): Future[Either[StatusMessage, String]] = Future {

    def getDefaultCollectionId(orgId: ObjectId): Validation[V2Error, String] = {
      orgCollectionService.getDefaultCollection(orgId).bimap(_ => generalError(""), _.id.toString)
    }

    val accessResult: Validation[V2Error, VersionedId[ObjectId]] = for {
      identity <- getOrgAndOptions(header)
      c <- collectionId.toSuccess(generalError("no collection id defined")).orElse(getDefaultCollectionId(identity.org.id))
      canWrite <- auth.canCreateInCollection(c)(identity)
      hasAccess <- if (canWrite) {
        Success(true)
      } else {
        Failure(generalError("Write to item denied"))
      }
      id <- mkItem(c, identity).toSuccess(generalError("Error creating item"))
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

