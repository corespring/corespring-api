package org.corespring.it.helpers

import org.bson.types.ObjectId
import org.corespring.models.Standard
import org.corespring.services.salat.bootstrap.CollectionNames

object StandardHelper extends CreateDelete[Standard] {
  lazy val mongoCollection = bootstrap.Main.db(CollectionNames.standard)
  implicit val ctx = bootstrap.Main.context
  override def id(thing: Standard): ObjectId = thing.id
}
