package web.controllers

import org.bson.types.ObjectId
import org.corespring.itemSearch._
import org.corespring.models.auth.Permission
import org.corespring.services.OrgCollectionService
import org.corespring.v2.actions.{ OrgRequest, V2Actions }
import play.api.libs.json.Json._
import play.api.libs.json.{ JsArray, Json }
import play.api.mvc.{ AnyContent, Controller }
import web.models.WebExecutionContext

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.{ Failure, Success }

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
  actions: V2Actions,
  searchService: ItemIndexService,
  orgCollectionService: OrgCollectionService,
  webExecutionContext: WebExecutionContext) extends Controller {

  implicit def ec: ExecutionContext = webExecutionContext.context

  def search(query: Option[String]) = actions.OrgAction.async { (request: OrgRequest[AnyContent]) =>

    QueryStringParser.scopedSearchQuery(query, request.org.accessibleCollections.map(_.collectionId)) match {
      case Success(q) => {

        val ids = request.org.accessibleCollections.map(_.collectionId)

        //Note: calling the future within a for-comprehension causes the execution to be sequential - we want parallel
        val futureSearchResult = searchService.search(q)
        val futurePermissions = orgCollectionService.getPermissions(request.org.id, ids: _*)

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
