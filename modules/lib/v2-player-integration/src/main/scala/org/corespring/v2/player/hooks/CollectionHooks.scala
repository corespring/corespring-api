package org.corespring.v2.player.hooks

import org.corespring.container.client.hooks.Hooks.StatusMessage
import org.corespring.container.client.hooks.{ CollectionHooks => ContainerCollectionHooks }
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.models.auth.Permission
import org.corespring.models.{ ContentCollection, Organization }
import org.corespring.services.ContentCollectionService
import org.corespring.v2.auth.LoadOrgAndOptions
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.errors.V2Error
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.RequestHeader

import scala.concurrent.Future
import scalaz.Validation

class CollectionHooks(
  colService: ContentCollectionService,
  getOrgAndOptsFn: RequestHeader => Validation[V2Error, OrgAndOpts],
  override implicit val ec: ContainerExecutionContext) extends ContainerCollectionHooks with LoadOrgAndOptions {

  lazy val logger = Logger(classOf[CollectionHooks])

  override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = getOrgAndOptsFn.apply(request)

  override def list()(implicit header: RequestHeader): Future[Either[StatusMessage, JsArray]] = Future {
    val result: Validation[V2Error, JsArray] = for {
      org <- getOrg(header)
      collections <- findWritableCollections(org)
    } yield toJsonArray(collections)

    result.leftMap { e => (e.statusCode -> e.message) }.toEither
  }

  private def toJsonArray(collections: Seq[ContentCollection]): JsArray = {
    Json.toJson(collections.map((c: ContentCollection) => Json.obj("key" -> c.id.toString, "value" -> c.name))).as[JsArray]
  }

  private def getOrg(h: RequestHeader): Validation[V2Error, Organization] = {
    getOrgAndOptions(h).map { o =>
      o.org
    }
  }

  private def findWritableCollections(org: Organization): Validation[V2Error, Seq[ContentCollection]] = {
    colService.getCollections(org.id, Permission.Write).leftMap {
      e => generalError(e.message)
    }
  }

}

