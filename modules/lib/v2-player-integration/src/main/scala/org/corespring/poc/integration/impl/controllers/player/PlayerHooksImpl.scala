package org.corespring.poc.integration.impl.controllers.player

import org.bson.types.ObjectId
import org.corespring.container.client.actions.{SessionIdRequest, PlayerRequest, ClientHooksActionBuilder}
import org.corespring.container.client.controllers.hooks.PlayerHooks
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.poc.integration.impl.actionBuilders.AuthenticatedSessionActions
import play.api.libs.json.{Json, JsString, JsValue}
import play.api.mvc.{Action, Result, AnyContent}
import scalaz.Scalaz._
import scalaz._

trait PlayerHooksImpl extends PlayerHooks {

  def sessionService: MongoService

  def itemService: ItemService

  def transformItem: Item => JsValue

  def auth: AuthenticatedSessionActions

  private def versionedId(json: JsValue): Option[VersionedId[ObjectId]] = json match {
    case JsString(s) => VersionedId(s)
    case _ => None
  }

  def builder: ClientHooksActionBuilder[AnyContent] = new ClientHooksActionBuilder[AnyContent] {

    private def maybeOid(s: String): Option[ObjectId] = if (ObjectId.isValid(s)) Some(new ObjectId(s)) else None

    private def load(sessionId: String)(block: PlayerRequest[AnyContent] => Result): Action[AnyContent] = auth.read(sessionId) {
      request =>

        val s: Validation[String, (Item, JsValue)] = for {

          oid <- maybeOid(sessionId).toSuccess("Invalid object id")
          session <- sessionService.load(oid.toString).toSuccess("Session Not found")
          vId <- versionedId(session \ "itemId").toSuccess("Can't parse item id")
          item <- itemService.findOneById(vId).toSuccess("Can't find item")
        } yield (item, session)

        s match {
          case Success(models) => {
            val (item, session) = models
            val itemJson = transformItem(item)
            block(PlayerRequest(itemJson, request, Some(session)))
          }
          case Failure(msg) => BadRequest(msg)
        }
    }

    def loadComponents(id: String)(block: (PlayerRequest[AnyContent]) => Result): Action[AnyContent] = load(id)(block)

    def loadServices(id: String)(block: (PlayerRequest[AnyContent]) => Result): Action[AnyContent] = load(id)(block)

    def loadConfig(id: String)(block: (PlayerRequest[AnyContent]) => Result): Action[AnyContent] = load(id)(block)

    def createSessionForItem(itemId: String)(block: (SessionIdRequest[AnyContent]) => Result): Action[AnyContent]
    = auth.createSessionHandleNotAuthorized(itemId) {
      request =>

        def createSessionJson(vid: VersionedId[ObjectId]) = {
          Some(
            Json.obj(
              "_id" -> Json.obj("$oid" -> JsString(ObjectId.get.toString)),
              "itemId" -> JsString(vid.toString)
            ))
        }

        val result = for {
          vid <- VersionedId(itemId).toSuccess(s"Error parsing item id: $itemId")
          json <- createSessionJson(vid).toSuccess("Error creating json")
          sessionId <- sessionService.create(json).toSuccess("Error creating session")
        } yield sessionId

        result match {
          case Success(sessionId) => block(SessionIdRequest(sessionId.toString, request))
          case Failure(msg) => BadRequest(msg)
        }
    }((r, msg) => Redirect("/login"))

  }
}
