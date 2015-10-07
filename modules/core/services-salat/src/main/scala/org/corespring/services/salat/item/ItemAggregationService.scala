package org.corespring.services.salat.item

import org.bson.types.ObjectId
import org.corespring.models.item.Item
import org.corespring.platform.data.mongo.SalatVersioningDao
import org.corespring.services.salat.bootstrap.SalatServicesExecutionContext
import org.corespring.{ services => interface }

import scala.concurrent.Future

//See: https://github.com/corespring/corespring-api/commit/a48aeeecc6df2e5ca852ee2b9956701d0044ab30
/*
val fieldValueMap = Map(
  "itemType" -> "taskInfo.itemType",
  "contributor" -> "contributorDetails.contributor")

def fieldValuesByFrequency(collectionIds: String, fieldName: String) = ApiActionRead { request =>

  import com.mongodb.casbah.map_reduce.MapReduceInlineOutput

  fieldValueMap.get(fieldName) match {
    case Some(field) => {
      // Expand "one.two" into Seq("this.one", "this.one.two") for checks down path in a JSON object
      val fieldCheck =
        field.split("\\.").foldLeft(Seq.empty[String])((acc, str) =>
          acc :+ (if (acc.isEmpty) s"this.$str" else s"${acc.last}.$str")).mkString(" && ")
      val cmd = MapReduceCommand(
        input = "content",
        map = s"""
          function() {
            if (${fieldCheck}) {
              emit(this.$field, 1);
            }
          }""",
        reduce = s"""
          function(previous, current) {
            var count = 0;
            for (index in current) {
              count += current[index];
            }
            return count;
          }""",
        query = Some(DBObject("collectionId" -> MongoDBObject("$in" -> collectionIds.split(",").toSeq))),
        output = MapReduceInlineOutput)

      ItemServiceWired.collection.mapReduce(cmd) match {
        case result: MapReduceInlineResult => {
          val fieldValueMap = result.map(_ match {
            case dbo: DBObject => {
              Some(dbo.get("_id").toString -> dbo.get("value").asInstanceOf[Double])
            }
            case _ => None
          }).flatten.toMap
          Ok(Json.prettyPrint(Json.toJson(fieldValueMap)))
        }
        case _ => BadRequest(Json.toJson(ApiError.InvalidField))
      }
    }
    case _ => BadRequest(Json.toJson(ApiError.InvalidField))
  }
}*/

class ItemAggregationService(itemDao: SalatVersioningDao[Item], ec: SalatServicesExecutionContext) extends interface.item.ItemAggregationService {

  implicit val executionContext = ec.ctx

  override def contributorCounts(collectionIds: Seq[ObjectId]): Future[Map[String, Int]] = Future {
    Map.empty
  }

  override def taskInfoItemTypeCounts(collectionIds: Seq[ObjectId]): Future[Map[String, Int]] = Future {
    Map.empty
  }
}
