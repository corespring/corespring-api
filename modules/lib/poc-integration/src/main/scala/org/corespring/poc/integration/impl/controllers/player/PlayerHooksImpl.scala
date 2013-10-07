package org.corespring.poc.integration.impl.controllers.player

import org.bson.types.ObjectId
import org.corespring.container.client.actions.{SessionIdRequest, PlayerRequest, ClientHooksActionBuilder}
import org.corespring.container.client.controllers.hooks.PlayerHooks
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.itemSession.{ItemSession, ItemSessionCompanion}
import org.corespring.platform.core.services.item.ItemService
import play.api.libs.json.JsValue
import play.api.mvc.{Action, Result, AnyContent}
import scalaz.Scalaz._
import scalaz._
import org.corespring.platform.data.mongo.models.VersionedId

trait PlayerHooksImpl extends PlayerHooks {

  def sessionService : ItemSessionCompanion

  def itemService : ItemService

  def transformItem : Item => JsValue

  def transformSession : ItemSession => JsValue


  def builder: ClientHooksActionBuilder[AnyContent] = new ClientHooksActionBuilder[AnyContent] {

    private def maybeOid (s:String) : Option[ObjectId] = if(ObjectId.isValid(s)) Some(new ObjectId(s)) else None

    private def load(sessionId:String)(block : PlayerRequest[AnyContent] => Result) : Action[AnyContent] = Action{ request =>

      val s : Validation[String, (Item,ItemSession)] = for{

        oid <- maybeOid(sessionId).toSuccess("Invalid object id")
        session <- sessionService.findOneById(oid).toSuccess("Not found")
        item <- itemService.findOneById(session.itemId).toSuccess("Can't find item")
      } yield (item,session)

      s match {
        case Success(models) => {
          val (item, session) = models
          val sessionJson = transformSession(session)
          val itemJson = transformItem(item)
          block(PlayerRequest(itemJson, request, Some(sessionJson)))
        }
        case Failure(msg) => BadRequest(msg)
      }
    }

    def loadComponents(id: String)(block: (PlayerRequest[AnyContent]) => Result): Action[AnyContent] = load(id)(block)

    def loadServices(id: String)(block: (PlayerRequest[AnyContent]) => Result): Action[AnyContent] = load(id)(block)

    def loadConfig(id: String)(block: (PlayerRequest[AnyContent]) => Result): Action[AnyContent] = load(id)(block)

    def createSessionForItem(itemId: String)(block: (SessionIdRequest[AnyContent]) => Result): Action[AnyContent] = Action{ request =>

      val result = for {
        vid <- VersionedId(itemId).toSuccess("Error creating item id")
        sessionId <- sessionService.insert(ItemSession(itemId = vid)).toSuccess("Error creating session")
      } yield sessionId

      result match {
        case Success(sessionId) =>  block(SessionIdRequest(sessionId.toString, request))
        case Failure(msg) =>  BadRequest(msg)
      }
    }
}
}
