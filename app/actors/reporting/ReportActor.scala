package actors.reporting

import akka.actor.Actor
import reporting.services.ReportsService
import play.Logger
import actors.reporting.ReportActor.ReportKeys
import scala.concurrent.{ExecutionContext, future}

/**
 * An Akka Actor responsible for generating and caching reports.
 */
class ReportActor(reportsService: ReportsService) extends Actor {

  /**
   * Cached data for all reports.
   *
   * TODO: These should be written to the filesystem if they start to take up too much memory.
   */
  private var reportCache = Map.empty[String, String]

  private val generators = Map(
    ReportKeys.primarySubject -> reportsService.buildPrimarySubjectReport,
    ReportKeys.standards -> reportsService.buildStandardsReport,
    ReportKeys.contributor -> reportsService.buildContributorReport,
    ReportKeys.collection -> (() => { "" })
  )

  def receive = { case _ => regenerateReports }

  def getReport(reportKey: String) = reportCache.get(reportKey)

  /**
   * Creates a report using the provided parameter-less function and adds results to cache. Logs to Logger.info,
   * Logger.error on completion.
   */
  def generateReport(reportKey: String) = {

    import ExecutionContext.Implicits.global

    reportCache = reportCache - reportKey

    future {
      try {
        generators.get(reportKey) match {
          case Some(generator: (() => String)) => {
            reportCache = reportCache + (reportKey -> generator())
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

  /**
   * Triggers regeneration of all reports
   */
  private def regenerateReports = generators
    .foreach{
      case (key, _) => generateReport(key)
    }

  regenerateReports
}

object ReportActor extends ReportActor(ReportsService) {

  object ReportKeys {
    val primarySubject = "primarySubject"
    val standards = "standards"
    val collection = "collection"
    val contributor = "contributor"
  }

}