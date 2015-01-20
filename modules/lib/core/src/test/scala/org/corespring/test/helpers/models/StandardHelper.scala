package org.corespring.test.helpers.models

import org.bson.types.ObjectId
import org.corespring.platform.core.models.{Standard, Subject}

object StandardHelper {

  def create(standards: Standard*): Seq[ObjectId] = {

    def saveStandard(s: Standard) = {
      Standard.save(s)
      s.id
    }
    standards.map(saveStandard)
  }

  def delete(ids: Seq[ObjectId]) = {
    ids.foreach(Standard.removeById(_))
  }

}
