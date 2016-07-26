package org.corespring.itemSearch

import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId

import scala.concurrent.Future
import scalaz.Validation

trait ItemIndexService {

  /**
   * Search - requires at least 1 collection id
   * @param query
   * @return
   */
  def search(query: ItemIndexQuery, preference: Option[String]): Future[Validation[Error, ItemIndexSearchResult]]
  def unboundedSearch(query: ItemIndexQuery, preference: Option[String]): Future[Validation[Error, ItemIndexSearchResult]]
  def distinct(field: String, collections: Seq[String] = Seq.empty): Future[Validation[Error, Seq[String]]]
  def reindex(id: VersionedId[ObjectId]): Future[Validation[Error, String]]
  def refresh(): Future[Validation[Error, String]]

  def componentTypes: Future[Validation[Error, Map[String, String]]]
  def widgetTypes: Future[Validation[Error, Map[String, String]]]

}

trait ItemIndexDeleteService {
  def delete(): Future[Validation[Error, Unit]]
  def create(): Future[Validation[Error, Unit]]
}
