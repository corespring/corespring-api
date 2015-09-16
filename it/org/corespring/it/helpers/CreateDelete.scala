package org.corespring.it.helpers

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.Context
import org.bson.types.ObjectId

private[helpers] trait CreateDelete[A <: AnyRef] {

  def mongoCollection: MongoCollection
  implicit def ctx: Context
  implicit def m: Manifest[A] = manifest[A]
  def id(thing: A): ObjectId

  val grate = com.novus.salat.grater[A]

  def create(thing: A): ObjectId = {
    mongoCollection.save(grate.asDBObject(thing))
    id(thing)
  }

  def create(things: A*): Seq[ObjectId] = {
    things.map(create)
  }

  def delete(ids: Seq[ObjectId]) = {
    val query = MongoDBObject("_id" -> MongoDBObject("$in" -> ids))
    mongoCollection.remove(query)
  }
}
