package api.v1

import controllers.auth.BaseApi
import play.api.mvc.Action
import play.api.libs.json.Json._
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.Play.current

/**
 * The Contributor API
 */
object ContributorApi extends BaseApi {
  def getContributorsList = ApiAction {
    request =>
      val collection = se.radley.plugin.salat.mongoCollection("content")
      val contributors = collection.distinct("contributorDetails.contributor")
      val contributorsFiltered = contributors.filter( _ != null )
      Ok(toJson(contributorsFiltered.map(p => JsObject(Seq("name" -> JsString(p.toString))))))
  }
}
