package internal.controllers

import controllers.auth.BaseApi
import play.mvc.BodyParser
import play.api.mvc.{ Action, AnyContentAsFormUrlEncoded, BodyParsers }
import play.mvc.BodyParser.FormUrlEncoded
import org.corespring.platform.data.VersioningDao
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.SalatVersioningDao
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.{ XmlSearchClient, XmlSearch, ItemServiceImpl }

trait QtiSearch extends BaseApi { self: XmlSearchClient =>

  def qtiSearchPage() = SecuredAction { request =>
    Ok(internal.views.html.qtiSearch())
  }

  def qtiSearch =
    ApiAction(p = BodyParsers.parse.urlFormEncoded) {
      request =>

        Organization.findOneById(request.ctx.organization) match {
          case Some(org) => {

            print(org.name)
            val ids = org.contentcolls.toList.map(_.collectionId.toString)
            request.body.get("text") match {
              case Some(Seq(text)) => {
                val items: List[Item] = xmlSearch.findInXml(text, ids)
                Ok(internal.views.html.qtiSearchResults(text, items))
              }
              case None => BadRequest("Bad parameters")
            }
          }
          case _ => BadRequest("no org found")
        }
    }
}

object QtiSearch extends QtiSearch with XmlSearchClient {
  def xmlSearch: XmlSearch = new XmlSearch {
    def dao: SalatVersioningDao[Item] = ItemServiceImpl.dao
  }
}
