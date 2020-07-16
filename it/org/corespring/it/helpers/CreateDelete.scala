package org.corespring.it.helpers

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import salat.Context
import grizzled.slf4j.Logger
import org.bson.types.ObjectId

private[helpers] trait CreateDelete[A <: AnyRef] {

  lazy val logger = Logger("it.helpers")

  def mongoCollection: MongoCollection
  implicit def ctx: Context
  def id(thing: A): ObjectId

  def create(thing: A)(implicit m: Manifest[A]): ObjectId = {
    val grate = salat.grater[A]
    mongoCollection.save(grate.asDBObject(thing))
    id(thing)
  }

  //TODO: @deprecated("Instead use the entity service.create directly", "")
  def create(things: A*)(implicit m: Manifest[A]): Seq[ObjectId] = {
    things.map(create)
  }

  //TODO: @deprecated("Instead use the entity service.delete directly", "")
  def delete(ids: Seq[ObjectId]) = {
    logger.info(s"deleting standards")
    val query = MongoDBObject("_id" -> MongoDBObject("$in" -> ids))
    mongoCollection.remove(query)
  }
}
