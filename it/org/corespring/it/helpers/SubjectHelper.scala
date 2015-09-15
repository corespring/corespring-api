package org.corespring.it.helpers

import com.mongodb.casbah.Imports
import com.novus.salat.Context
import org.bson.types.ObjectId
import org.corespring.models.Subject
import org.corespring.services.salat.bootstrap.CollectionNames

object SubjectHelper extends CreateDelete[Subject] {
  lazy val mongoCollection: Imports.MongoCollection = bootstrap.Main.db(CollectionNames.subject)

  override implicit def ctx: Context = bootstrap.Main.context

  override def id(thing: Subject): ObjectId = thing.id
}
