package org.corespring.v2player.integration.controllers.editor

import org.bson.types.ObjectId
import org.corespring.container.client.actions._
import org.corespring.container.client.controllers.resources.Item
import org.corespring.platform.core.models
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.item.resource.{ VirtualFile, Resource }
import org.corespring.platform.core.models.item.{ Item => ModelItem }
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2player.integration.actionBuilders.LoadOrgAndOptions
import org.corespring.v2player.integration.errors.Errors.{ orgCantAccessCollection, noOrgIdAndOptions, propertyNotFoundInJson, noJson }
import org.corespring.v2player.integration.errors.V2Error
import play.api.libs.json.JsValue
import play.api.mvc.{ Action, Result, AnyContent }
import scalaz.{ Failure, Validation, Success }

object ItemWithActions {}

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

    def save(itemId: String)(block: (SaveItemRequest[AnyContent]) => Result): Action[AnyContent] = Action(BadRequest("Not ready yet"))

    def getScore(itemId: String)(block: (ScoreItemRequest[AnyContent]) => Result): Action[AnyContent] = ???

    private def createItem(collectionId: String): Option[VersionedId[ObjectId]] = {

      val item = models.item.Item(
        collectionId = collectionId,
        data = Some(Resource(
          name = "data",
          files = Seq(
            VirtualFile(
              name = "qti.xml",
              contentType = "text/xml",
              content = "<assessmentItem><itemBody><p>I'm a new item</p></itemBody></assessmentItem>")))))
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
