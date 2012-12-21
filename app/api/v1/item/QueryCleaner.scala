package api.v1.item

import models.ContentCollection
import play.api.libs.json.{JsString, Json, JsValue}
import com.mongodb.casbah.Imports._
import controllers.auth.Permission
import models.Item._


/**
 * Process item queries to ensure that they only query against collections that the
 * users' organization has access to.
 */
object QueryCleaner {

  def clean(q: Option[String], orgId: ObjectId): DBObject = {

    val enforcedQuery = restrictedQueryForOrg(orgId)

    def processCollectionIds(dbo: DBObject): DBObject = {

      if (!dbo.contains(collectionId)) {
        dbo.putAll(enforcedQuery.toMap)
      } else {
        dbo.get(collectionId) match {
          case s: String => {
            if (!isIdValid(s, orgId)) {
              dbo.putAll(enforcedQuery.toMap)
            }
          }
          case inDbo: DBObject if inDbo.get("$in") != null => {

            val ids: BasicDBList = inDbo.get("$in").asInstanceOf[BasicDBList]
            val trimmed = ids.map {
              id =>
                if (isIdValid(id.toString, orgId)) Some(id) else None
            }.flatten
            inDbo.put("$in", trimmed)
          }
        }
      }
      dbo
    }

    q match {
      case Some(s) => {
        try {
          val obj: Any = com.mongodb.util.JSON.parse(s)
          processCollectionIds(obj.asInstanceOf[DBObject])
        }
        catch {
          case e: Throwable => new BasicDBObject(enforcedQuery.toMap)
        }
      }
      case _ => new BasicDBObject(enforcedQuery.toMap)
    }
  }

  private def isIdValid(collectionId: String, orgId: ObjectId): Boolean = {
    collectionIdsForOrg(orgId).map(_.toString).contains(collectionId)
  }

  private def restrictedQueryForOrg(orgId: ObjectId): MongoDBObject = {
    val collectionIds = collectionIdsForOrg(orgId).map(_.toString)
    MongoDBObject("collectionId" -> MongoDBObject("$in" -> collectionIds))
  }

  private def collectionIdsForOrg(orgId: ObjectId): Seq[ObjectId] =
    ContentCollection.getCollectionIds(orgId, Permission.All, false)

}
