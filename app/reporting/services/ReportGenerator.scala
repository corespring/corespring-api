package reporting.services

import scala.concurrent._
import scala.Some
import play.Logger
import play.cache.Cache
import reporting.services.ReportGenerator.ReportKeys

class ReportGenerator(reportsService: ReportsService) {

  val generatorFunctions = Map(
    ReportKeys.primarySubject -> reportsService.buildPrimarySubjectReport,
    ReportKeys.standards -> reportsService.buildStandardsReport,
    ReportKeys.contributor -> reportsService.buildContributorReport,
    ReportKeys.collection -> (() => { "" })
  )

  /**
   * Triggers generation of all reports
   */
  def generateAllReports = generatorFunctions.foreach { case (key, _) => generateReport(key) }

  def getReport(reportKey: String): Option[String] = Option(Cache.get(reportKey)) match {
    case Some(report: String) => Some(report)
    case None => None
  }

  /**
   * Creates a report using the provided parameter-less function and adds results to cache. Logs to Logger.info,
   * Logger.error on completion.
   */
  def generateReport(reportKey: String) = {

    import ExecutionContext.Implicits.global

    Cache.remove(reportKey)

    future {
      Logger.info(s"Starting to generate report $reportKey")
      try {
        generatorFunctions.get(reportKey) match {
          case Some(generator: (() => String)) => {
            Cache.set(reportKey, generator())
            Logger.info(s"Generated $reportKey report")
          }
          case _ => Logger.warn(s"Cannot process unknown report $reportKey")
        }
      } catch {
        case e: Exception => {
          Logger.error(s"There was an error generating the $reportKey report")
          e.printStackTrace
        }
      }
    }
  }

}

object ReportGenerator extends ReportGenerator(ReportsService) {

  object ReportKeys {
    val primarySubject = "primarySubject"
    val standards = "standards"
    val collection = "collection"
    val contributor = "contributor"
  }

}
