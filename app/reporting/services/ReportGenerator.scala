package reporting.services

import scala.concurrent._
import scala.Some
import play.Logger
import play.cache.Cache
import reporting.services.ReportGenerator.ReportKeys
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}

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

  def getReport(reportKey: String): Option[(DateTime, Option[String], Boolean)] = Option(Cache.get(reportKey)) match {
    case Some((date: DateTime, report: Option[String], inProgress: Boolean)) => Some(date, report, inProgress)
    case _ => None
  }

  def inProgress(reportKey: String): Boolean = getReport(reportKey) match {
    case Some((_, _, inProgress)) => inProgress
    case None => false
  }

  def inProgress(): Map[String, Boolean] = ReportKeys.keys.map(key => (key, inProgress(key))).toMap

  def timestamps: Map[String, String] = {
    ReportKeys.keys.map(key => (key, (getReport(key) match {
      case Some((date: DateTime, Some(report), _)) => Some(date.toString("MM/dd/YYYY hh:mm aa"))
      case _ => None
    }))).filter{ case (a,b) => b.nonEmpty }.map{ case (a,b) => (a, b.get) }.toMap
  }

  /**
   * Creates a report using the provided parameter-less function and adds results to cache. Logs to Logger.info,
   * Logger.error on completion.
   */
  def generateReport(reportKey: String): Future[Option[String]] = {

    import ExecutionContext.Implicits.global

    Option(Cache.get(reportKey)) match {
      case Some((date, report, _)) => Cache.set(reportKey, (date, report, true))
      case _ => Cache.set(reportKey, (new DateTime, None, true))
    }

    future {
      Logger.info(s"Starting to generate report $reportKey")
      try {
        generatorFunctions.get(reportKey) match {
          case Some(generator: (() => String)) => {
            val report = generator()
            Cache.set(reportKey, (new DateTime, Some(report), false))
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

    val keys = Seq(primarySubject, standards, collection, contributor)
  }

}
