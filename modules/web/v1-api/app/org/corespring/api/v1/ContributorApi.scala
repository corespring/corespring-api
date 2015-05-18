package org.corespring.api.v1

import com.mongodb.DBObject
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.{MongoDBObjectBuilder, MongoDBObject}
import org.corespring.platform.core.controllers.auth.BaseApi
import org.corespring.platform.core.models.ContentCollection
import org.corespring.platform.core.services.item._
import play.api.Play.current
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json._

import se.radley.plugin.salat.mongoCollection
import scala.collection.JavaConverters._

/**
 * The Contributor API
 */
class ContributorApi(contentCollection: MongoCollection, itemService: ItemService) extends BaseApi {
  import org.corespring.platform.core.models.auth.Permission.Read

  implicit class WithPlus(one: DBObject) {
    def +(two: DBObject) = one.keySet.asScala.foldLeft(
      two.keySet.asScala.foldLeft(new MongoDBObjectBuilder()) { (acc, key) => {
        acc += (key -> two.get(key))
      }}
    ){ (acc, key) => acc += (key -> one.get(key))}.result
  }

  def getContributorsList = ApiAction {
    request =>
      val canAccessCollection = itemService.createDefaultCollectionsQuery(
        ContentCollection.getCollectionIds(request.ctx.organization, Read), request.ctx.organization)
      val queryBuilder = canAccessCollection.keySet.asScala.foldLeft(new MongoDBObjectBuilder()){ (acc, key) => {
        acc += (key -> canAccessCollection.get(key))
      }}
      val query = canAccessCollection + withoutArchive + MongoDBObject("contentType" -> "item")
      val contributors = contentCollection.distinct("contributorDetails.contributor", query).map(_.toString)
      val contributorsFiltered = contributors.filter(c => c != null)
      Ok(toJson(contributorsFiltered.map(p => JsObject(Seq("name" -> JsString(p))))))
  }
}

object ContributorApi extends ContributorApi(mongoCollection("content"), ItemServiceWired)