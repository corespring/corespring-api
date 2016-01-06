package org.corespring.passage.search

import scala.concurrent.Future
import scalaz.Validation

trait PassageIndexService {

  def search(query: PassageIndexQuery): Future[Validation[Error, PassageIndexSearchResult]]

}
