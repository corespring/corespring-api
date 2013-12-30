package tests.reporting.services

import org.specs2.mutable.Specification
import org.specs2.mock.Mockito
import reporting.services.{ReportGenerator, ReportsService}
import reporting.services.ReportGenerator.ReportKeys
import org.corespring.test.PlaySingleton
import scala.concurrent.Await
import scala.concurrent.duration._

class ReportGeneratorTest extends Specification with Mockito {

  PlaySingleton.start()

  "generateAllReports" should {

    val reportsService = mock[ReportsService]
    val reportGenerator = new ReportGenerator(reportsService)

    "call all generator functions" in {
      reportGenerator.generateAllReports.foreach(Await.ready(_, Duration(1000, MILLISECONDS)))
      there was one(reportsService).buildPrimarySubjectReport
      there was one(reportsService).buildStandardsReport
      there was one(reportsService).buildContributorReport
    }

  }

  "generateReport" should {

    val reportsService = mock[ReportsService]
    val reportGenerator = new ReportGenerator(reportsService)

    "call generator function for primarySubject" in {
      Await.ready(reportGenerator.generateReport(ReportKeys.primarySubject), Duration(1000, MILLISECONDS))
      there was one(reportsService).buildPrimarySubjectReport
    }

    "call generator function for standards" in {
      Await.ready(reportGenerator.generateReport(ReportKeys.standards), Duration(1000, MILLISECONDS))
      there was one(reportsService).buildStandardsReport
    }

    "call generator function for collection" in {
      pending
    }

    "call generator function for contributor" in {
      Await.ready(reportGenerator.generateReport(ReportKeys.contributor), Duration(1000, MILLISECONDS))
      there was one(reportsService).buildContributorReport
    }

  }

}
