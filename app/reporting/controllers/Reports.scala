package reporting.controllers

import controllers.auth.BaseApi
import org.corespring.platform.core.models.{ Subject, Standard, ContentCollection }
import reporting.services.ReportsService
import org.corespring.platform.core.services.item.ItemServiceImpl
import actors.reporting.ReportActor
import play.api.mvc.{SimpleResult, Action}
import actors.reporting.ReportActor.ReportKeys

object Reports extends BaseApi {

  private val service = ReportsService
  private val actor = ReportActor


  def index = ApiAction {
    request =>
      val availableCollections = service.getCollections
      Ok(reporting.views.html.index(availableCollections))
  }

  def refresh(reportKey: String) = ApiAction { request =>
    actor.generateReport(reportKey)
    getStatus(reportKey)
  }

  def refreshStatus(reportKey: String) = ApiAction { request => getStatus(reportKey) }

  private def getStatus(reportKey: String) = {
    actor.getReport(reportKey) match {
      case Some(report) => Ok("")
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
      //text/csv
      Ok(out).withHeaders(("Content-type", "text/csv"))
  }

  def getPrimarySubjectItemReport = ApiAction(request => getReport(ReportKeys.primarySubject))
  def getStandardItemReport = ApiAction(request => getReport(ReportKeys.standards))
  def getContributorReport = ApiAction(request => getReport(ReportKeys.contributor))
  def getCollectionReport = ApiAction(request => getReport(ReportKeys.collection))

  private def getReport(reportKey: String): SimpleResult = actor.getReport(reportKey) match {
    case Some(string) => Ok(string).withHeaders(("Content-type", "text/csv"))
    case _ => InternalServerError("There was an error generating this report. Please check the logs.")
  }

}
