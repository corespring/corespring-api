package org.corespring.test.fakes

import com.mongodb.{ WriteResult, WriteConcern, BasicDBObject, DBCollection }
import com.mongodb.casbah.{ Imports, MongoCollection => CasbahMongoCollection }
import org.specs2.mock.Mockito

object Fakes extends Mockito {

  class MongoCollection(n: Int) extends CasbahMongoCollection(mock[DBCollection]) {
    var queryObj: BasicDBObject = null
    var updateObj: BasicDBObject = null

    override def update[A, B](q: A, o: B, upsert: Boolean, multi: Boolean, concern: WriteConcern)(implicit queryView: (A) => Imports.DBObject, objView: (B) => Imports.DBObject, encoder: Imports.DBEncoder): WriteResult = {
      queryObj = q.asInstanceOf[BasicDBObject]
      updateObj = o.asInstanceOf[BasicDBObject]
      mock[WriteResult].getN returns n
    }
  }
}

