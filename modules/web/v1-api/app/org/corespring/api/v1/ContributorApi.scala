package org.corespring.api.v1

import org.corespring.platform.core.controllers.auth.BaseApi
import org.corespring.services.item.ItemService
import play.api.libs.json.{ Json }
import play.api.libs.json.Json._

class ContributorApi(itemService: ItemService) extends BaseApi {

  def getContributorsList = ApiAction {
    request =>
      val contributors = itemService.contributorsForOrg(request.ctx.orgId)
        .map(c => (Json.obj("name" -> c)))
      Ok(toJson(contributors))
  }
}

