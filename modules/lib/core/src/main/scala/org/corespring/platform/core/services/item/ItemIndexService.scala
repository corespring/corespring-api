package org.corespring.platform.core.services.item

import org.corespring.platform.core.models.item.index.ItemIndexSearchResult

import scala.concurrent.Future
import scalaz.Validation

trait ItemIndexService {

  def search(query: ItemIndexQuery): Future[Validation[Error, ItemIndexSearchResult]]

}
