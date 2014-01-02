package org.corespring.reporting.services

import play.Logger
import play.cache.Cache
import scala.Some
import scala.concurrent._
import org.corespring.reporting.services.ReportGenerator.ReportKeys

class ReportGenerator(reportsService: ReportsService) {

  val generatorFunctions = Map(
    ReportKeys.primarySubject -> reportsService.buildPrimarySubjectReport _,
    ReportKeys.standards -> reportsService.buildStandardsReport _,
    ReportKeys.contributor -> reportsService.buildContributorReport _,
    ReportKeys.collection -> reportsService.buildCollectionReport _
  )

  /**
   * Triggers generation of all reports
   */
  def generateAllReports = generatorFunctions.map { case (key, _) => generateReport(key) }.toSeq

  def getReport(reportKey: String): Option[String] = Option(Cache.get(reportKey)) match {
    case Some(report: String) => Some(report)
    case _ => None
  }

  /**
   * Creates a report using the provided parameter-less function and adds results to cache. Logs to Logger.info,
   * Logger.error on completion.
   */
  def generateReport(reportKey: String): Future[Option[String]] = {

    import ExecutionContext.Implicits.global

    Cache.remove(reportKey)

    future {
      Logger.info(s"Starting to generate report $reportKey")
      try {
        generatorFunctions.get(reportKey) match {
          case Some(generator: (() => String)) => {
            val report = generator()
            Cache.set(reportKey, report)
            Logger.info(s"Generated $reportKey report")
            Some(report)
          }
          case _ => {
            Logger.warn(s"Cannot process unknown report $reportKey")
            None
          }
        }
      } catch {
        case e: Exception => {
          Logger.error(s"There was an error generating the $reportKey report")
          e.printStackTrace
          None
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
