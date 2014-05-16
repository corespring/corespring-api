package org.corespring.api.v2.services

import org.corespring.platform.core.models.item.Item

trait V2ItemService {

  def create(item:Item) : Option[Item]

}
