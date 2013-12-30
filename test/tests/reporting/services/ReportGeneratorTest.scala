package tests.reporting.services

import org.specs2.mutable.Specification
import org.specs2.mock.Mockito
import reporting.services.{ReportGenerator, ReportsService}
import reporting.services.ReportGenerator.ReportKeys
import org.corespring.test.PlaySingleton

class ReportGeneratorTest extends Specification with Mockito {

  PlaySingleton.start()

  "generateAllReports" should {

    val reportsService = mock[ReportsService]
    val reportGenerator = new ReportGenerator(reportsService)

    "call all generator functions" in {
      reportGenerator.generateAllReports
      there was one(reportsService).buildPrimarySubjectReport
      there was one(reportsService).buildStandardsReport
      there was one(reportsService).buildContributorReport
    }

  }

  "generateReport" should {

    val reportsService = mock[ReportsService]
    val reportGenerator = new ReportGenerator(reportsService)

    "call generator function for primarySubject" in {
      reportGenerator.generateReport(ReportKeys.primarySubject)
      there was one(reportsService).buildPrimarySubjectReport
    }

    "call generator function for standards" in {
      reportGenerator.generateReport(ReportKeys.standards)
      there was one(reportsService).buildStandardsReport
    }

    "call generator function for collection" in {
      pending
    }

    "call generator function for contributor" in {
      reportGenerator.generateReport(ReportKeys.contributor)
      there was one(reportsService).buildContributorReport
    }

  }

}
