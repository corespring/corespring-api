package org.corespring.services.salat

import com.mongodb.DBObject
import com.mongodb.casbah.WriteConcern
import com.mongodb.casbah.commons.{ MongoDBList, MongoDBObject }
import com.novus.salat.Context
import com.novus.salat.dao.SalatDAO
import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.models.Subject
import org.corespring.services
import org.corespring.services.SubjectQuery
import play.api.libs.json.{ JsValue, Json }

class SubjectService(dao: SalatDAO[Subject, ObjectId], context: Context) extends services.SubjectService {

  private val logger = Logger(classOf[SubjectService])

  override def findOneById(id: ObjectId): Option[Subject] = dao.findOneById(id)

  override def findOne(id: String): Option[Subject] = if (ObjectId.isValid(id)) {
    logger.trace(s"findOne: $id")
    dao.findOneById(new ObjectId(id))
  } else None

  override def list(l: Int = 0, sk: Int = 0): Stream[Subject] = {
    logger.trace(s"list")
    dao.find(MongoDBObject.empty).limit(l).skip(sk).toStream
  }

  override def query(term: SubjectQuery, l: Int, sk: Int): Stream[Subject] = {
    val query = toDbo(term)
    dao.find(query).skip(sk).limit(l).toStream
  }

  override def count(query: DBObject): Long = dao.count(query)

  override def find(dbo: DBObject): Stream[Subject] = dao.find(dbo).toStream

  override def insert(s: Subject): Option[ObjectId] = dao.insert(s, WriteConcern.Safe)

  override def delete(id: ObjectId): Boolean = dao.removeById(id).getN == 1

  private def toDbo(q: SubjectQuery): DBObject = {

    val exactMatchQuery = MongoDBObject(
      List(q.category.map("category" -> _),
        q.subject.map("subject" -> _)).flatten)

    def matchAgainstTermQuery(key: String, value: Option[String]): Option[DBObject] = {
      if (value.isEmpty) Some(MongoDBObject(key -> toRegex(q.term))) else None
    }

    val orObject = List(
      matchAgainstTermQuery("category", q.category),
      matchAgainstTermQuery("subject", q.subject)).flatten

    import com.mongodb.casbah.Imports._

    exactMatchQuery ++ $or(orObject)
  }

  private def toRegex(searchTerm: String) = MongoDBObject("$regex" -> searchTerm, "$options" -> "i")

}
