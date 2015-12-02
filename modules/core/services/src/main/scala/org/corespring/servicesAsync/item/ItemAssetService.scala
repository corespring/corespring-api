package org.corespring.servicesAsync.item

import org.corespring.models.item.Item
import org.corespring.models.item.resource.CloneFileResult

import scalaz.Validation
import scala.concurrent.Future

trait ItemAssetService {
  def delete(key: String): Future[Unit]
  def cloneStoredFiles(from: Item, to: Item): Future[Validation[Seq[CloneFileResult], Item]]
}
