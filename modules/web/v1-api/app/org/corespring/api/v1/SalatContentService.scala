package org.corespring.api.v1

import com.mongodb.casbah.Imports._
import com.novus.salat.dao.SalatMongoCursor

import org.corespring.models.item.Content
import org.corespring.services.item.BaseContentService

trait SalatContentService[ContentType <: Content[ID], ID] extends BaseContentService[ContentType, ID] {

  def find(query: DBObject, fields: DBObject = new BasicDBObject()): SalatMongoCursor[ContentType]
}
