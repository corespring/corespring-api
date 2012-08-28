package controllers.web.services

import com.mongodb.casbah.MongoCollection
import com.mongodb.{BasicDBList, DBObject, BasicDBObject}
import com.mongodb.util.JSON
import models.web.User
import controllers.web.utils.ConfigLoader

object UserService {
  def login(username: String, password: String): Option[User] = {
    val collection: MongoCollection = DBConnect.getCollection(ConfigLoader.get("MONGO_URI").get, "users")
    val query: BasicDBObject = new BasicDBObject()
    query.append("username", username)
    query.append("password", password)
    val result: Option[DBObject] = collection.findOne(query)

    result match {
      case Some(dbObject) => {
        Some(
          User(
            dbObject.get("username").asInstanceOf[String],
            dbObject.get("password").asInstanceOf[String])
        )
      }
      case _ => None
    }
  }

  def count(): Int = {
    val collection: MongoCollection = DBConnect.getCollection(ConfigLoader.get("MONGO_URI").get, "users")
    collection.count.toInt
  }

  def insertFromJson(json: String): Boolean = {
    val collection: MongoCollection = DBConnect.getCollection(ConfigLoader.get("MONGO_URI").get, "users")
    val data: BasicDBList = JSON.parse(json).asInstanceOf[BasicDBList]

    val iterator: java.util.Iterator[Object] = data.iterator()
    do {
      val item = iterator.next
      collection.insert(item.asInstanceOf[DBObject])
    } while (iterator.hasNext)
    true
  }
}
