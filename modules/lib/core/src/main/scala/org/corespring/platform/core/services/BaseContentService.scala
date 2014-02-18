package org.corespring.platform.core.services

import org.corespring.platform.core.models.item.Content
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.TypeImports.BasicDBObject
import com.novus.salat.dao.SalatMongoCursor
import org.corespring.platform.core.models.error

trait BaseContentService[ContentType <: Content, ID] {

  def clone(content: ContentType): Option[ContentType]

  def findFieldsById(id: ID, fields: DBObject = new BasicDBObject()): Option[DBObject]

  def currentVersion(id: ID): Option[Int]

  def count(query: DBObject, fields: Option[String] = None): Int

  def find(query: DBObject, fields: DBObject = new BasicDBObject()): SalatMongoCursor[ContentType]

  def findOneById(id: ID): Option[ContentType]

  def findOne(query: DBObject): Option[ContentType]

  def save(i: ContentType, createNewVersion: Boolean = false)

  /** Save using a dbo - allows finer grained updates using $set */
  def saveUsingDbo(id: ID, dbo: DBObject, createNewVersion: Boolean = false)

  def insert(i: ContentType): Option[ID]

  def findMultiple(ids: Seq[ID], keys: DBObject = new BasicDBObject()): Seq[ContentType]

  def sessionCount(content: ContentType): Long

  def createDefaultCollectionsQuery[A](collections: Seq[ObjectId], orgId: ObjectId): MongoDBObject

  def parseCollectionIds[A](organizationId: ObjectId)(value: AnyRef): Either[error.InternalError, AnyRef]
}

