package org.corespring.platform.core.services

import org.corespring.platform.core.models.item.Content
import com.mongodb.casbah.Imports._
import org.corespring.platform.core.models._
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.error.InternalError
import com.mongodb.casbah.commons.MongoDBObject
import org.corespring.platform.core.models.item.Item.Keys._
import com.novus.salat.dao.SalatMongoCursor
import scala.Some

trait BaseContentService[ContentType <: Content[ID], ID] {

  def clone(content: ContentType): Option[ContentType]

  def currentVersion(id: ID): Option[Int] = throw new RuntimeException("to be implemented?")

  def count(query: DBObject, fields: Option[String] = None): Int

  def find(query: DBObject, fields: DBObject = new BasicDBObject()): SalatMongoCursor[ContentType]

  def findOneById(id: ID): Option[ContentType]

  def findOne(query: DBObject): Option[ContentType]

  def save(i: ContentType, createNewVersion: Boolean = false)

  def insert(i: ContentType): Option[ID]

  def findMultiple(ids: Seq[ID], keys: DBObject = new BasicDBObject()): Seq[ContentType]

  def createDefaultCollectionsQuery[A](collections: Seq[ObjectId], orgId: ObjectId): MongoDBObject = {
    // filter the collections to exclude any that are not currently enabled for the organization
    val org = Organization.findOneById(orgId)
    val disabledCollections: Seq[ObjectId] = org match {
      case Some(organization) => organization.contentcolls.filterNot(collRef => collRef.enabled).map(_.collectionId)
      case None => Seq()
    }
    val enabledCollections = collections.filterNot(disabledCollections.contains(_))
    val collectionIdQry: MongoDBObject = MongoDBObject(collectionId -> MongoDBObject("$in" -> enabledCollections.map(_.toString)))
    val sharedInCollectionsQry: MongoDBObject = MongoDBObject(sharedInCollections -> MongoDBObject("$in" -> enabledCollections))
    val initSearch: MongoDBObject = MongoDBObject("$or" -> MongoDBList(collectionIdQry, sharedInCollectionsQry))
    initSearch
  }

  def parseCollectionIds[A](organizationId: ObjectId)(value: AnyRef): Either[error.InternalError, AnyRef] = value match {
    case dbo: BasicDBObject => dbo.toSeq.headOption match {
      case Some((key, dblist)) => if (key == "$in") {
        if (dblist.isInstanceOf[BasicDBList]) {
          try {
            if (dblist.asInstanceOf[BasicDBList].toArray.forall(coll => ContentCollection.isAuthorized(organizationId, new ObjectId(coll.toString), Permission.Read)))
              Right(value)
            else Left(InternalError("attempted to access a collection that you are not authorized to"))
          } catch {
            case e: IllegalArgumentException => Left(InternalError("could not parse collectionId into an object id", e))
          }
        } else Left(InternalError("invalid value for collectionId key. could not cast to array"))
      } else Left(InternalError("can only use $in special operator when querying on collectionId"))
      case None => Left(InternalError("empty db object as value of collectionId key"))
    }
    case _ => Left(InternalError("invalid value for collectionId"))
  }

}

