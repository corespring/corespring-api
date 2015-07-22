package org.corespring.services

import org.bson.types.ObjectId
import org.corespring.models.{ Domain, Standard }

trait StandardService extends QueryService[Standard] {

  def findOneById(id: ObjectId): Option[Standard]

  def domains: Map[String, Seq[Domain]]

  def findOneByDotNotation(dotNotation: String): Option[Standard]
}
