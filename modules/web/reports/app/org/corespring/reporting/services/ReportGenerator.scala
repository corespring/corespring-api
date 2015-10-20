package org.corespring.reporting.services

import org.corespring.reporting.services.ReportGenerator.ReportKeys
import org.joda.time.DateTime
import play.Logger
import scala.concurrent._
import play.libs.Akka
import play.api.cache.Cache

object ExecutionContexts {
  import play.api.Play.current
  lazy val reportGeneration = Akka.system.dispatchers.lookup("akka.report-generator.generate-report")
}

class ReportGenerator(reportsService: ReportsService) {

  val generatorFunctions = Map(
    ReportKeys.primarySubject -> reportsService.buildPrimarySubjectReport _,
    ReportKeys.standards -> reportsService.buildStandardsReport _,
    ReportKeys.contributor -> reportsService.buildContributorReport _,
    ReportKeys.collection -> reportsService.buildCollectionReport _,
    ReportKeys.standardsByCollection -> reportsService.buildStandardsByCollectionReport _,
    ReportKeys.subcategory -> reportsService.buildStandardsGroupReport _)

  /**
   * Triggers generation of all reports
   */
  def generateAllReports = generatorFunctions.map { case (key, _) => generateReport(key) }.toSeq

  def getReport(reportKey: String): Option[(DateTime, Option[String], Boolean)] = Option(play.api.Cache.get(reportKey)) match {
    case Some((date: DateTime, report, inProgress)) => Some(date, report.asInstanceOf[Option[String]], inProgress.asInstanceOf[Boolean])
    case _ => None
  }

  def inProgress(reportKey: String): Boolean = getReport(reportKey) match {
    case Some((_, _, inProgress)) => inProgress
    case None => false
  }

  def inProgress(): Map[String, Boolean] = ReportKeys.keys.map(key => (key, inProgress(key))).toMap

  def timestamps: Map[String, String] = {
    ReportKeys.keys.map(key => (key, (getReport(key) match {
      case Some((date: DateTime, Some(report), _)) => Some(date.toString("MM/dd/YYYY hh:mm aa z"))
      case _ => None
    }))).filter { case (a, b) => b.nonEmpty }.map { case (a, b) => (a, b.get) }.toMap
  }

  /**
   * Creates a report using the provided parameter-less function and adds results to cache. Logs to Logger.info,
   * Logger.error on completion.
   */
  def generateReport(reportKey: String): Future[Option[String]] = {

    implicit val executionContext: ExecutionContext = ExecutionContexts.reportGeneration

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
      } finally {
        Logger.info(s"Finished attempt to generate $reportKey report")
      }
    }
  }

}

object ReportGenerator extends ReportGenerator(ReportsService) {

  object ReportKeys {
    val primarySubject = "primarySubject"
    val standards = "standards"
    val subcategory = "subcategory"
    val collection = "collection"
    val contributor = "contributor"
    val standardsByCollection = "standardsByCollection"

    val keys = Seq(primarySubject, standards, subcategory, collection, contributor, standardsByCollection)
  }

}
