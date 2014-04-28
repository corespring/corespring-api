package org.corespring.api.v1

import org.corespring.platform.core.controllers.auth.BaseApi
import play.api.Play.current
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json._

/**
 * The Contributor API
 */
object ContributorApi extends BaseApi {
  def getContributorsList = ApiAction {
    request =>
      val collection = se.radley.plugin.salat.mongoCollection("content")
      val contributors = collection.distinct("contributorDetails.contributor")
      val contributorsFiltered = contributors.filter((c: Any) => c != null)
      Ok(toJson(contributorsFiltered.map((p: Any) => JsObject(Seq("name" -> JsString(p.toString))))))
  }
}
