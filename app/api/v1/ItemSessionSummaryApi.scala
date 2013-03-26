package api.v1

import controllers.auth.BaseApi
import org.bson.types.ObjectId
import models.itemSession.{ItemSessionSummary, ItemSession}
import models.item.{Item, Content}
import controllers.auth.Permission
import play.api.libs.json.Json._
import com.mongodb.casbah.commons.MongoDBObject

object ItemSessionSummaryApi extends BaseApi {

  def multiple = ApiAction {
    request => {

      def isAuthorized(orgId: ObjectId)(session: ItemSession): Boolean = {
        Content.isAuthorized(orgId, session.itemId, Permission.Read)
      }

      def emptyArray = Ok("[]").withHeaders(("Content-Type", "application/json"))

      request.body.asJson match {
        case Some(json) => {
          (json \ "ids").asOpt[Seq[String]] match {
            case Some(ids) => {
              val oids = ids.map(new ObjectId(_))
              val sessions = ItemSession.findMultiple(oids)
              val filtered = sessions.filter(isAuthorized(request.ctx.organization))

              val itemIds = filtered.map(_.itemId).distinct

              val items = Item.findMultiple(
                itemIds,
                MongoDBObject("taskInfo.title" -> 1, "standards" -> 1))

              val sessionSummaries = filtered.map((is: ItemSession) => {
                items.find(_.id == is.itemId) match {
                  case Some(item) => Some(ItemSessionSummary(is, item))
                  case _ => None
                }
              }).flatten
              val json = toJson(sessionSummaries)
              Ok(json)
            }
            case _ => emptyArray
          }
        }
        case _ => emptyArray
      }
    }
  }

}
