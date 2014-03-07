package api.v1

import org.corespring.search.ItemSearch
import org.corespring.search.elasticsearch.ElasticSearchItemSearch
import controllers.auth.BaseApi
import play.api.mvc.Action
import scala.concurrent._
import play.api.libs.json.Json

class ItemSearchApi(itemSearch: ItemSearch) extends BaseApi {
  import ExecutionContext.Implicits.global

  def find(query: Option[String]) = Action.async {
    itemSearch.find(
      queryString = query,
      collectionIds = Seq(),
      fields = Seq("_id", "published", "collectionId", "priorGradeLevel", "dateModified", "taskInfo"),
      sort = None
    ).map(json => Ok(Json.prettyPrint(json)))
  }

}

object ItemSearchApi extends ItemSearchApi(ElasticSearchItemSearch)
