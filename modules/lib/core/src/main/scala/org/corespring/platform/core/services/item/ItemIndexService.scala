package org.corespring.platform.core.services.item

import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.index.ItemIndexSearchResult
import org.corespring.platform.data.mongo.models.VersionedId

import scala.concurrent.Future
import scalaz.Validation

trait ItemIndexService {

  def search(query: ItemIndexQuery): Future[Validation[Error, ItemIndexSearchResult]]
  def distinct(field: String, collections: Seq[String] = Seq.empty): Future[Validation[Error, Seq[String]]]
  def reindex(id: VersionedId[ObjectId]): Future[Validation[Error, String]]
  def refresh(): Future[Validation[Error, String]]

  def componentTypes: Future[Validation[Error, Map[String, String]]]

}
