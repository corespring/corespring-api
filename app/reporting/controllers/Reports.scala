package reporting.controllers

import controllers.auth.BaseApi
import reporting.services.{ReportGenerator, ReportsService}
import play.api.mvc.SimpleResult
import reporting.services.ReportGenerator.ReportKeys
import play.api.libs.json.Json
import net.quux00.simplecsv.CsvReader
import java.io.StringReader
import scala.collection.JavaConversions._
import org.bson.types.ObjectId
import org.corespring.platform.core.models.ContentCollection
import org.corespring.platform.core.models.auth.Permission
import reporting.utils.CsvWriter
import com.mongodb.casbah.Imports._
import scala.Some
import play.api.mvc.SimpleResult

class Reports(service: ReportsService, generator: ReportGenerator) extends BaseApi with CsvWriter {

  def index = ApiAction {
    request =>
      val availableCollections = service.getCollections
      Ok(reporting.views.html.index(availableCollections, generator.timestamps, generator.inProgress))
  }

  def generate(reportKey: String) = ApiAction { request =>
    generator.generateReport(reportKey)
    getStatus(reportKey)
  }

  def status(reportKey: String) = ApiAction { request => getStatus(reportKey) }

  private def getStatus(reportKey: String) = generator.inProgress(reportKey) match {
    case true => Accepted("")
    case _ => generator.getReport(reportKey) match {
      case Some((date, _, false)) => Ok(Json.obj("timestamp" -> date.toString("MM/dd/YYYY hh:mm aa z")))
      case _ => Accepted("")
    }
  }

  def getCsv(collection: String, queryType: String) = ApiAction {
    request =>
      logger.info("getCsv: " + collection + " type: " + queryType)
      val result = service.getReport(collection, queryType)
      val out = ("" /: result) {
        (a, i) => a + i._1 + "," + i._2 + "\n"
      }
      logger.info(out)
      Ok(out).withHeaders(("Content-type", "text/csv"))
  }

  def getPrimarySubjectItemReport = ApiAction(request => getReport(ReportKeys.primarySubject))
  def getStandardItemReport = ApiAction(request => getReport(ReportKeys.standards))
  def getContributorReport = ApiAction(request => getReport(ReportKeys.contributor))
  def getCollectionReport = ApiAction(request => getReport(ReportKeys.collection))

  /**
   * Only report current user's visible collections
   */
  def getStandardsByCollectionReport = ApiAction {
    request => {
      val collections: Seq[ContentCollection] =
        ContentCollection.find(
          MongoDBObject("_id" -> MongoDBObject(
            "$in" -> ContentCollection.getContentCollRefs(request.ctx.organization, Permission.Read, true).map(_.collectionId))
          )
        ).toSeq

      generator.getReport(ReportKeys.standardsByCollection) match {
        case Some((date, Some(string), inProgress)) => {
          val csvReader = new CsvReader(new StringReader(string))
          val lines = csvReader.readAll.toList
          val header = lines.head
          val whitelist: Seq[Int] = 0 +: header.tail.map(new ObjectId(_)).zipWithIndex
            .filter{ case (id, index) => collections.map(_.id).contains(id) }.map{ case (id, index) => index + 1}
          val filtered = lines.map(_.zipWithIndex.filter{case (_, index) => whitelist.contains(index) }
            .map{ case (string, _) => string }.toList)
          val withHeaders = filtered.head.map( header => {
            header match {
              case "Standards" => header
              case _ => collections.find(_.id == new ObjectId(header)).getOrElse(throw new RuntimeException("OMG")).name
            }
          }) +: filtered.tail
          Ok(withHeaders.toCsv).withHeaders(("Content-type", "text/csv"), ("Content-disposition", s"attachment; file=${ReportKeys.standardsByCollection}.csv"))
        }
        case _ => InternalServerError("There was an error generating this report. Please check the logs.")
      }
    }
  }


  private def getReport(reportKey: String): SimpleResult = generator.getReport(reportKey) match {
    case Some((date, Some(string), inProgress)) =>
      Ok(string).withHeaders(("Content-type", "text/csv"), ("Content-disposition", s"attachment; file=$reportKey.csv"))
    case _ => InternalServerError("There was an error generating this report. Please check the logs.")
  }

}

object Reports extends Reports(ReportsService, ReportGenerator)