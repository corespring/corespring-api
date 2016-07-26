package org.corespring.v2.player.hooks

import org.corespring.container.client.hooks.Hooks._
import org.corespring.container.client.hooks.{ComponentEditorHooks => ContainerComponentEditorHooks}
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.conversion.qti.transformers.ItemTransformer
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.ItemService
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.auth.{ItemAuth, LoadOrgAndOptions}
import org.corespring.v2.errors.V2Error
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz._

class ComponentEditorHooks(
  itemService: ItemService,
  auth: ItemAuth[OrgAndOpts],
  getOrgAndOptsFn: RequestHeader => Validation[V2Error, OrgAndOpts],
  override implicit val containerContext: ContainerExecutionContext) extends ContainerComponentEditorHooks  {


  override def load(itemId: String)(implicit header: RequestHeader): Future[Either[(Int, String), (JsValue, JsValue)]] = Future {
    Right((Json.obj(), Json.obj()))
  }


}
