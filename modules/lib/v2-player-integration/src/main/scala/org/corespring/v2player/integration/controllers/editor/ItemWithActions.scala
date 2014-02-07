package org.corespring.v2player.integration.controllers.editor

import org.bson.types.ObjectId
import org.corespring.container.client.actions._
import org.corespring.container.client.controllers.resources.Item
import org.corespring.platform.core.models
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.item.{Item => ModelItem, PlayerDefinition}
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2player.integration.actionBuilders.LoadOrgAndOptions
import org.corespring.v2player.integration.errors.Errors._
import org.corespring.v2player.integration.errors.V2Error
import play.api.libs.json.{JsError, JsSuccess, Json, JsValue}
import play.api.mvc.{Action, Result, AnyContent}
import scalaz.{Failure, Validation, Success}
import org.corespring.container.client.actions.SaveItemRequest
import org.corespring.container.client.actions.NewItemRequest
import org.corespring.container.client.actions.ScoreItemRequest
import org.corespring.v2player.integration.errors.Errors.propertyNotFoundInJson
import scalaz.Failure
import scala.Some
import org.corespring.container.client.actions.ItemRequest
import org.corespring.v2player.integration.errors.Errors.noOrgIdAndOptions
import scalaz.Success
import org.corespring.v2player.integration.errors.Errors.orgCantAccessCollection

trait ItemWithActions
  extends Item
  with LoadOrgAndOptions {

  def itemService: ItemService

  def orgService: OrganizationService

  def transform: ModelItem => JsValue

  override def actions: ItemActions[AnyContent] = new ItemActions[AnyContent] {

    def load(itemId: String)(block: (ItemRequest[AnyContent]) => Result): Action[AnyContent] = Action {
      request =>
        val item = for {
          id <- VersionedId(itemId)
          item <- itemService.findOneById(id)
        } yield item

        item.map {
          i =>
            val pocJson = transform(i)
            block(ItemRequest(pocJson, request))
        }.getOrElse(NotFound("?"))
    }

    def save(itemId: String)(block: (SaveItemRequest[AnyContent]) => Result): Action[AnyContent] = Action {
      request =>

        import scalaz._
        import scalaz.Scalaz._

        val out: Validation[V2Error, Result] = for {
          vid <- VersionedId(itemId).toSuccess(cantParseItemId)
          item <- itemService.findOneById(vid).toSuccess(cantFindItemWithId(vid))
        } yield {

          /** an implementation for the container to save its definition */
          def save(itemId: String, playerJson: JsValue): Option[JsValue] = {
            val playerDef = playerJson.as[PlayerDefinition]
            val update = item.copy(playerDefinition = Some(playerDef))
            itemService.save(update, false)
            Some(playerJson)
          }

          val itemJson = item.playerDefinition.map {
            Json.toJson(_)
          }.getOrElse(Json.obj())

          block(SaveItemRequest(itemJson, save, request))
        }

        out match {
          case Success(r) => r
          case Failure(err) => Status(err.code)(err.message)
        }
    }

    def getScore(itemId: String)(block: (ScoreItemRequest[AnyContent]) => Result): Action[AnyContent] = ???

    private def createItem(collectionId: String): Option[VersionedId[ObjectId]] = {

      val definition = PlayerDefinition(Seq(), "<div>I'm a new item</div>", Json.obj())
      val item = models.item.Item(
        collectionId = collectionId,
        playerDefinition = Some(definition))

      itemService.insert(item)
    }

    override def create(error: (Int, String) => Result)(block: (NewItemRequest[AnyContent]) => Result): Action[AnyContent] = Action {
      request =>

        import scalaz.Scalaz._

        val accessResult: Validation[V2Error, String] = for {
          json <- request.body.asJson.toSuccess(noJson)
          collectionId <- (json \ "collectionId").asOpt[String].toSuccess(propertyNotFoundInJson("collectionId"))
          orgIdAndOptions <- getOrgIdAndOptions(request).toSuccess(noOrgIdAndOptions(request))
          access <- if (orgService.canAccessCollection(orgIdAndOptions._1, new ObjectId(collectionId), Permission.Write)) {
            Success(true)
          } else {
            Failure(orgCantAccessCollection(orgIdAndOptions._1, collectionId))
          }
        } yield collectionId

        accessResult match {
          case Success(collectionId) => {
            createItem(collectionId).map {
              id =>
                block(NewItemRequest(id.toString, request))
            }.getOrElse(error(BAD_REQUEST, "Access failed"))
          }
          case Failure(err) => error(err.code, err.message)
        }

    }
  }
}
