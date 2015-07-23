package actors.reporting

import akka.actor.Actor
import org.corespring.reporting.services.ReportGenerator

/**
 * An Akka Actor responsible for generating and caching reports.
 */
class ReportActor(reportGenerator: ReportGenerator) extends Actor {

  def receive = { case _ => reportGenerator.generateAllReports }

}