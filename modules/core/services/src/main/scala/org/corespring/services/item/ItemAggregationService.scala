package org.corespring.services.item

import org.bson.types.ObjectId

import scala.concurrent.Future

trait ItemAggregationService {

  def contributorCounts(collectionIds: Seq[ObjectId]): Future[Map[String, Int]]

  def taskInfoItemTypeCounts(collectionIds: Seq[ObjectId]): Future[Map[String, Int]]
}
