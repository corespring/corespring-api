package org.corespring.it.helpers

import global.Global.main
import com.mongodb.casbah.Imports
import salat.Context
import org.bson.types.ObjectId
import org.corespring.models.Subject
import org.corespring.services.salat.bootstrap.CollectionNames

object SubjectHelper extends CreateDelete[Subject] {
  lazy val mongoCollection: Imports.MongoCollection = main.db(CollectionNames.subject)

  override implicit def ctx: Context = main.context

  override def id(thing: Subject): ObjectId = thing.id
}
