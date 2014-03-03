package api.v1

import org.corespring.search.indexing.ItemSearch
import org.corespring.search.elasticsearch.ElasticSearchItemSearch
import controllers.auth.BaseApi
import play.api.mvc.Action
import scala.concurrent._

class ItemSearchApi(itemSearch: ItemSearch) extends BaseApi {
  import ExecutionContext.Implicits.global

  def find(query: Option[String]) = Action.async {
    query match {
      case Some(queryString) => itemSearch.find(queryString).map(Ok(_))
      case _ => future { BadRequest("include a query, guy.") }
    }
  }

}

object ItemSearchApi extends ItemSearchApi(ElasticSearchItemSearch)
