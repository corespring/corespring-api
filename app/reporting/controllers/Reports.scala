package reporting.controllers

import controllers.auth.BaseApi
import reporting.services.{ReportGenerator, ReportsService}
import play.api.mvc.SimpleResult
import reporting.services.ReportGenerator.ReportKeys

class Reports(service: ReportsService, generator: ReportGenerator) extends BaseApi {

  def index = ApiAction {
    request =>
      val availableCollections = service.getCollections
      Ok(reporting.views.html.index(availableCollections))
  }

  def generate(reportKey: String) = ApiAction { request =>
    generator.generateReport(reportKey)
    getStatus(reportKey)
  }

  def status(reportKey: String) = ApiAction { request => getStatus(reportKey) }

  private def getStatus(reportKey: String) = {
    generator.getReport(reportKey) match {
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

  private def getReport(reportKey: String): SimpleResult = generator.getReport(reportKey) match {
    case Some(string) => Ok(string).withHeaders(("Content-type", "text/csv"))
    case _ => InternalServerError("There was an error generating this report. Please check the logs.")
  }

}

object Reports extends Reports(ReportsService, ReportGenerator)