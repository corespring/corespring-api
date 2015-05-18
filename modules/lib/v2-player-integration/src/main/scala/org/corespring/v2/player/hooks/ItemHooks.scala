package org.corespring.v2.player.hooks

import org.bson.types.ObjectId
import org.corespring.container.client.hooks.Hooks.StatusMessage
import org.corespring.container.client.hooks.{ItemHooks => ContainerItemHooks}
import org.corespring.platform.core.models.item.{Item => ModelItem, PlayerDefinition}
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.auth.{ItemAuth, LoadOrgAndOptions}
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import play.api.libs.json._
import play.api.mvc.RequestHeader

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz.{Failure, Success, Validation}

trait ItemHooks extends ContainerItemHooks with LoadOrgAndOptions {

  def transform: ModelItem => JsValue

  def auth: ItemAuth[OrgAndOpts]

  lazy val logger = V2LoggerFactory.getLogger("ItemHooks")

  override def load(itemId: String)(implicit header: RequestHeader): Future[Either[StatusMessage, JsValue]] = Future {
    val item: Validation[V2Error, JsValue] = for {
      identity <- getOrgAndOptions(header)
      vid <- VersionedId(itemId).toSuccess(cantParseItemId(itemId))
      item <- auth.loadForRead(itemId)(identity)
    } yield transform(item)

    item.leftMap(e => e.statusCode -> e.message).toEither
  }

  override def create(maybeJson: Option[JsValue])(implicit header: RequestHeader): Future[Either[StatusMessage, String]] = Future {

    def createItem(collectionId: String, identity: OrgAndOpts): Option[VersionedId[ObjectId]] = {
      val definition = PlayerDefinition(Seq(), "<div>I'm a new item</div>", Json.obj(), "", None)
      val item = ModelItem(
        collectionId = Some(collectionId),
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

