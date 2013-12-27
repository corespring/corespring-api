package actors.reporting

import akka.actor.Actor
import reporting.services.{ReportGenerator, ReportsService}

/**
 * An Akka Actor responsible for generating and caching reports.
 */
class ReportActor(reportGenerator: ReportGenerator) extends Actor {

  def receive = { case _ => reportGenerator.generateAllReports }

}