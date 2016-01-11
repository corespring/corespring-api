package web.controllers

import org.bson.types.ObjectId
import org.corespring.itemSearch._
import org.corespring.models.auth.Permission
import org.corespring.services.OrgCollectionService
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.V2Error
import play.api.libs.json.{ JsArray, Json }
import play.api.libs.json.Json._
import play.api.mvc.{ RequestHeader, Request, AnyContent, Controller }
import org.corespring.v2.actions.V2Actions
import web.models.WebExecutionContext
import scala.concurrent.{ ExecutionContext, Future }
import scalaz.{ Failure, Success, Validation }

case class ItemInfo(hit: ItemIndexHit, p: Option[Permission]) {
  def json = {
    val permissionJson = p.map { permission =>
      obj("permission" ->
        obj("read" -> permission.read,
          "write" -> permission.write,
          "clone" -> permission.canClone))
    }.getOrElse(Json.obj())

    ItemIndexHit.Format.writes(hit).deepMerge(permissionJson)
  }
}

object ItemInfo {
  def apply(permissions: Seq[(ObjectId, Option[Permission])])(hit: ItemIndexHit): ItemInfo = {

    val itemPermission = for {
      c <- hit.collectionId
      cid <- if (ObjectId.isValid(c)) Some(new ObjectId(c)) else None
      p <- permissions.find {
        case (collectionId, maybePermission) =>
          collectionId == cid
      }
      permission <- p._2
    } yield permission

    ItemInfo(hit, itemPermission)
  }
}

class ItemSearch(
  searchService: ItemIndexService,
  orgCollectionService: OrgCollectionService,
  webExecutionContext: WebExecutionContext,
  getOrgAndOptsFn: RequestHeader => Validation[V2Error, OrgAndOpts]) extends Controller with V2Actions {

  override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = getOrgAndOptsFn(request)

  override implicit def ec: ExecutionContext = webExecutionContext.context

  def search(query: Option[String]) = OrgAction { (orgAndOpts: OrgAndOpts, request: Request[AnyContent]) =>

    QueryStringParser.scopedSearchQuery(query, orgAndOpts.org.accessibleCollections.map(_.collectionId)) match {
      case Success(q) => {

        val ids = orgAndOpts.org.accessibleCollections.map(_.collectionId)

        val futureSearchResult = searchService.search(q)
        val futurePermissions = orgCollectionService.getPermissions(orgAndOpts.org.id, ids: _*)

        for {
          searchResult <- futureSearchResult
          permissions <- futurePermissions
        } yield {
          searchResult match {
            case Success(result) => {
              val info = result.hits.map(ItemInfo(permissions))
              val json = obj("total" -> result.total, "hits" -> JsArray(info.map(_.json)))
              Ok(json)
            }
            case Failure(e) => BadRequest(e.getMessage)
          }
        }
      }
      case Failure(e) => Future(BadRequest(e.getMessage))
    }
  }
}
