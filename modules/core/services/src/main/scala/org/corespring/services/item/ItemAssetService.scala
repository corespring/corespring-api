package org.corespring.services.item

import org.corespring.models.item.Item
import org.corespring.models.item.resource.CloneFileResult

import scalaz.Validation

trait ItemAssetService {
  def delete(key: String): Unit
  def cloneStoredFiles(from: Item, to: Item): Validation[Seq[CloneFileResult], Item]
}
