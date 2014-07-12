package org.corespring.v2.api.services

import org.corespring.platform.core.models.item.Item

trait V2ItemService {

  def create(item: Item): Option[Item]

}
