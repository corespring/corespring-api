package web.controllers

import org.corespring.itemSearch.{ ItemIndexQuery, ItemIndexService }
import org.corespring.models.ContentCollRef
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.V2Error
import play.api.mvc.{ RequestHeader, Request, AnyContent, Controller }
import org.corespring.v2.actions.V2Actions
import scala.concurrent.Future
import scalaz.{ Failure, Success, Validation }

class ItemSearch(
  searchService: ItemIndexService,
  getOrgAndOptsFn: RequestHeader => Validation[V2Error, OrgAndOpts]) extends Controller with V2Actions {

  override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = getOrgAndOptsFn(request)

  def scopedSearchQuery(query: Option[String], accessibleCollections: Seq[ContentCollRef]): Validation[V2Error, ItemIndexQuery] = {
    ???
  }

  def search(query: Option[String]) = OrgAction { (orgAndOpts: OrgAndOpts, request: Request[AnyContent]) =>

    scopedSearchQuery(query, orgAndOpts.org.accessibleCollections) match {
      case Success(q) => {
        searchService.search(q).map { result =>
          Ok("")
        }
      }
      case Failure(e) => {
        Future(BadRequest(e.message))
      }
    }
  }
}
