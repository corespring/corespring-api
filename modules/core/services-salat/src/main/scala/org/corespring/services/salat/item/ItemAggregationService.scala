package org.corespring.services.salat.item

import com.mongodb.casbah.Imports._
import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.services.salat.bootstrap.SalatServicesExecutionContext
import org.corespring.{ services => interface }

import scala.concurrent.Future

/**
 * Port of the logic from CollectionApi
 * See: https://github.com/corespring/corespring-api/commit/a48aeeecc6df2e5ca852ee2b9956701d0044ab30
 */
class ItemAggregationService(collection: MongoCollection, ec: SalatServicesExecutionContext) extends interface.item.ItemAggregationService {

  implicit val executionContext = ec.ctx

  private lazy val logger = Logger(classOf[ItemAggregationService])

  private def mkPropertyTest(field: String) = {
    val parts = field.split("\\.")
    def addBits(acc: Seq[String], str: String) = acc :+ s"${acc.last}.$str"
    val added = parts.foldLeft(Seq("this"))(addBits).drop(1)
    added.mkString(" && ")
  }

  private def propertyCounts(key: String, collectionIds: Seq[ObjectId]): Future[Map[String, Double]] = Future {

    val map = s"""function() {
                    if (${mkPropertyTest(key)}) {
                      emit(this.$key, 1);
                    }
                  }"""

    val reduce = s"""function(previous, current) {
                       var count = 0;
                       for (index in current) {
                         count += current[index];
                       }
                       return count;
                     }"""

    val query = "collectionId" $in collectionIds.map(_.toString)
    val cmd = MapReduceCommand("content", map, reduce, MapReduceInlineOutput, Some(query))

    def mkKeyValue(dbo: DBObject) = for {
      key <- dbo.expand[String]("_id")
      value <- dbo.expand[Double]("value")
    } yield key -> value

    collection.mapReduce(cmd) match {
      case inline: MapReduceInlineResult => {
        logger.trace(s"function=propertyCounts, result=$inline")
        inline.flatMap(mkKeyValue).toMap
      }
      case _ => Map.empty
    }
  }

  override def contributorCounts(collectionIds: Seq[ObjectId]): Future[Map[String, Double]] = {
    propertyCounts("contributorDetails.contributor", collectionIds)
  }

  override def taskInfoItemTypeCounts(collectionIds: Seq[ObjectId]): Future[Map[String, Double]] = {
    propertyCounts("taskInfo.itemType", collectionIds)
  }
}
