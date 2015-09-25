package org.corespring.services

import com.mongodb.DBObject
import org.bson.types.ObjectId
import org.corespring.models.{ Standard }

trait StandardService extends QueryService[Standard] {

  def delete(id: ObjectId): Boolean

  def insert(standard: Standard): Option[ObjectId]

  def count(query: DBObject): Long

  def findOneById(id: ObjectId): Option[Standard]

  def findOneByDotNotation(dotNotation: String): Option[Standard]
}
