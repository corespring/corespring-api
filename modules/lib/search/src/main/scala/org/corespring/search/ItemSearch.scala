package org.corespring.search.indexing

import play.api.libs.json.JsArray
import scala.concurrent.Future
import org.bson.types.ObjectId

trait ItemSearch {

  def find(queryString: Option[String],
           collectionIds: Seq[ObjectId] = Seq.empty,
           fields: Seq[String] = Seq.empty,
           skip: Option[Int] = None,
           limit: Option[Int] = None,
           sort: Option[String]): Future[JsArray]

}
