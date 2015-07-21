package org.corespring.services.item

import org.corespring.models.item.FieldValue

trait FieldValueService {

  def get: Option[FieldValue]

}
