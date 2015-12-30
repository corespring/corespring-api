package org.corespring.it.helpers

import global.Global.main
import org.bson.types.ObjectId
import org.corespring.models.Standard
import org.corespring.services.salat.bootstrap.CollectionNames

object StandardHelper extends CreateDelete[Standard] {
  lazy val mongoCollection = main.db(CollectionNames.standard)
  implicit val ctx = main.context
  override def id(thing: Standard): ObjectId = thing.id
}
