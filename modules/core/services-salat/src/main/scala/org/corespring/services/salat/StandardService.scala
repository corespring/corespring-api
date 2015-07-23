package org.corespring.services.salat

import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.Context
import com.novus.salat.dao.SalatDAO
import org.bson.types.ObjectId
import org.corespring.models.{Standard, Domain}

class StandardService(
                       val dao : SalatDAO[Standard,ObjectId],
                       val context : Context
                       ) extends org.corespring.services.StandardService with HasDao[Standard,ObjectId]{

  override def findOneById(id: ObjectId): Option[Standard] = dao.findOneById(id)

  //TODO: RF - implement
  override def domains: Map[String, Seq[Domain]] = ???

  override def findOneByDotNotation(dotNotation: String): Option[Standard] = dao.findOne(MongoDBObject("dotNotation" -> dotNotation))

  override def query(term: String): Seq[Standard] = ???

  override def findOne(id: String): Option[Standard] = ???

  override def list(): Seq[Standard] = ???
}
