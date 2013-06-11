package models.item.service

import models.item.Item
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.platform.data.VersioningDao

trait XmlSearchClient{
  def xmlSearch : XmlSearch

}

trait XmlSearch {

  def dao : VersioningDao[Item,ObjectId]

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
