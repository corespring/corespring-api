package org.corespring.v2player.integration

import com.mongodb.casbah.MongoDB
import org.corespring.container.client.actions._
import org.corespring.container.client.component._
import org.corespring.container.client.controllers.{ Assets, DataQuery => ContainerDataQuery }
import org.corespring.container.components.model.Component
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.v2player.integration.auth.SessionAuth
import org.corespring.v2player.integration.hooks.{ SessionHooks => ApiSessionHooks }
import org.corespring.platform.core.controllers.auth.SecureSocialService
import play.api.libs.json.JsValue
import play.api.mvc._
import play.api.{ Configuration, Logger }
import securesocial.core.{ Identity, SecureSocial }

class V2PlayerIntegration(comps: => Seq[Component],
  val configuration: Configuration,
  db: MongoDB)
  extends org.corespring.container.client.integration.DefaultIntegration {

  lazy val logger = Logger("v2player.integration")

  override def components: Seq[Component] = comps

  lazy val secureSocialService = new SecureSocialService {
    override def currentUser(request: RequestHeader): Option[Identity] = SecureSocial.currentUser(request)
  }

  override def componentUrls: ComponentUrls = ???

  override def assets: Assets = ???

  override def dataQuery: ContainerDataQuery = ???

  override def sessionHooks: SessionHooks = new ApiSessionHooks {
    override def auth: SessionAuth = ???

    override def itemService: ItemService = ???

    override def transformItem: (Item) => JsValue = ???

    override def sessionService: MongoService = ???
  }

  override def itemHooks: ItemHooks = ???

  override def playerLauncherActions: PlayerLauncherActions[AnyContent] = ???

  override def catalogActions: CatalogActions[AnyContent] = ???

  override def playerActions: PlayerActions[AnyContent] = ???

  override def editorActions: EditorActions[AnyContent] = ???
}
