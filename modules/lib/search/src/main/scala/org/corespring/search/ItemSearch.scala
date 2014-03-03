package org.corespring.search.indexing

import play.api.libs.json.JsValue
import scala.concurrent.Future

trait ItemSearch {

  def find(query: String): Future[JsValue]

}
