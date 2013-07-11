package models.item.service

import models.item.Item
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.platform.data.VersioningDao
import org.corespring.platform.data.mongo.SalatVersioningDao

trait XmlSearchClient{
  def xmlSearch : XmlSearch

}

trait XmlSearch {

  def dao : SalatVersioningDao[Item]

  def findInXml(string: String, collectionIds: List[String]): List[Item] = {

    val query = string

    dao.find(
      MongoDBObject(
        "data.files.content" ->
          MongoDBObject(
            "$regex" -> query,
            "$options" -> "msi"),
        "collectionId" -> MongoDBObject("$in" -> collectionIds.toArray)
      ),
      MongoDBObject("taskInfo" -> 1)
    ).toList
  }

}
