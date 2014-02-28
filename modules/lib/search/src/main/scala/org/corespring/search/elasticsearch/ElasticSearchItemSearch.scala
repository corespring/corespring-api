package org.corespring.search.elasticsearch

import org.corespring.search.indexing.ItemSearch
import play.api.libs.json.Json

object ElasticSearchItemSearch extends ItemSearch {

  def search(query: String) = {
    Json.obj()
  }

}
