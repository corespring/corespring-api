package org.corespring.test.fakes

import com.mongodb._
import com.mongodb.casbah.{ Imports, MongoCollection => CasbahMongoCollection }
import org.specs2.mock.Mockito

object Fakes extends Mockito {

  class MongoCollection(var n: Int) extends CasbahMongoCollection(mock[DBCollection]) {
    var queryObj: DBObject = null
    var updateObj: DBObject = null
    var fieldsObj: DBObject = null
    var sortObj: DBObject = null

    override def update[A, B](q: A, o: B, upsert: Boolean, multi: Boolean, concern: WriteConcern)(implicit queryView: (A) => Imports.DBObject, objView: (B) => Imports.DBObject, encoder: Imports.DBEncoder): WriteResult = {
      queryObj = q.asInstanceOf[BasicDBObject]
      updateObj = o.asInstanceOf[BasicDBObject]
      mock[WriteResult].getN returns n
    }

    var findAndModifyResult: Option[DBObject] = None

    override def findAndModify[A, B, C, D](
      query: A,
      fields: B,
      sort: C,
      remove: Boolean,
      update: D,
      returnNew: Boolean,
      upsert: Boolean)(implicit ac: A => DBObject,
        bc: B => DBObject,
        cc: C => DBObject,
        dc: D => DBObject): Option[DBObject] = {
      queryObj = ac(query)
      fieldsObj = bc(fields)
      sortObj = cc(sort)
      updateObj = dc(update)
      findAndModifyResult
    }
  }
}

