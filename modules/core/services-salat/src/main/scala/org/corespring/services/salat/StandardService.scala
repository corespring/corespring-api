package org.corespring.services.salat

import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.models.{ Standard, Domain }

trait StandardService extends org.corespring.services.StandardService with HasDao[Standard,ObjectId]{

  override def findOneById(id: ObjectId): Option[Standard] = dao.findOneById(id)

  //TODO: RF - implement
  override def domains: Map[String, Seq[Domain]] = ???

  override def findOneByDotNotation(dotNotation: String): Option[Standard] = dao.findOne(MongoDBObject("dotNotation" -> dotNotation))
}
