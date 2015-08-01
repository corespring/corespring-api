package org.corespring.api.v1

import org.corespring.legacy.ServiceLookup
import org.corespring.platform.core.controllers.auth.BaseApi
import org.corespring.services.metadata.ContributorsService
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json._

/**
 * The Contributor API
 */
class ContributorApi(contributorsService: ContributorsService) extends BaseApi {

  /*implicit class WithPlus(one: DBObject) {
    def +(two: DBObject) = one.keySet.asScala.foldLeft(
      two.keySet.asScala.foldLeft(new MongoDBObjectBuilder()) { (acc, key) => {
        acc += (key -> two.get(key))
      }}
    ){ (acc, key) => acc += (key -> one.get(key))}.result
  }*/

  def getContributorsList = ApiAction {
    request =>

      //TODO- move to salat services
      /*val canAccessCollection = itemService.createDefaultCollectionsQuery(
        contentCollectionService.getCollectionIds(request.ctx.organization, Permission.Read), request.ctx.organization)
      val queryBuilder = canAccessCollection.keySet.asScala.foldLeft(new MongoDBObjectBuilder()){ (acc, key) => {
        acc += (key -> canAccessCollection.get(key))
      }}
      //val query = canAccessCollection + withoutArchive + MongoDBObject("contentType" -> "item")
      //"contributorDetails.contributor", query).map(_.toString)
      val contributorsFiltered = contributors.filter(c => c != null)*/
      val contributors = contributorsService.contributorsForOrg(request.ctx.organization)
      Ok(toJson(contributors.map(p => JsObject(Seq("name" -> JsString(p))))))
  }
}

object ContributorApi extends ContributorApi(ServiceLookup.contributorsService)