package org.corespring.platform.core.services

import org.bson.types.ObjectId
import org.corespring.common.log.PackageLogging
import org.corespring.platform.core.models.{ Standard }

object StandardQueryService extends QueryService[Standard] with StandardQueryBuilder with PackageLogging {

  override def findOne(id: String): Option[Standard] = if (ObjectId.isValid(id)) {
    logger.trace(s"findOne: $id")
    Standard.findOneById(new ObjectId(id))
  } else None

  override def list(): Seq[Standard] = {
    logger.trace(s"list")
    Standard.findAll().toSeq
  }

  override def query(raw: String): Seq[Standard] = {
    getQuery(raw).map(query => {
      logger.trace(s"mongo query: ${query}")
      Standard.find(query).toSeq
    }).getOrElse(Seq.empty[Standard])
  }

  def getQuery(raw: String) = {
    getStandardByDotNotationQuery(raw).orElse(getStandardBySearchQuery(raw))
  }

}
