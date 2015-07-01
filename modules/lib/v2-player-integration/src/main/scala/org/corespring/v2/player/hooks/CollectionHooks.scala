package org.corespring.v2.player.hooks

import org.corespring.container.client.hooks.Hooks.StatusMessage
import org.corespring.container.client.hooks.{ CollectionHooks => ContainerCollectionHooks }
import org.corespring.platform.core.models.{ Organization, ContentCollection }
import org.corespring.platform.core.models.auth.Permission
import org.corespring.v2.auth.services.{ ContentCollectionService, OrgService }
import org.corespring.v2.auth.{ LoadOrgAndOptions }
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import play.api.libs.json._
import play.api.mvc.{ RequestHeader }

import scala.concurrent.Future

import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

trait CollectionHooks extends ContainerCollectionHooks with LoadOrgAndOptions {

  lazy val logger = V2LoggerFactory.getLogger("CollectionHooks")

  def colService: ContentCollectionService

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
    try {
      Success(colService.getCollections(org, Permission.Write).toSeq)
    } catch {
      case e: Throwable => Failure(generalError("Error finding collections"))
    }
  }
}

