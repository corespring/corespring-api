package org.corespring.search.indexing

import play.api.libs.json.JsValue

trait ItemSearch {

  def search(query: String): JsValue


}
