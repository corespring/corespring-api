package org.corespring.v2.api

import org.corespring.itemSearch.ItemIndexService
import org.corespring.services.{ SubjectService, StandardService }
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.V2Error
import org.mockito.Matchers
import org.specs2.specification.Scope
import play.api.test.FakeRequest

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.{ Failure, Success, Validation }

class FieldValuesApiTest extends V2ApiSpec {

  import ExecutionContext.Implicits.global

  val contributorValues = Seq("these", "are", "contributors")
  val gradeValues = Seq("these are grades")

  class apiScope(override val orgAndOpts: Validation[V2Error, OrgAndOpts] = Success(mockOrgAndOpts())) extends Scope with V2ApiScope {

    lazy val itemIndexService = {
      val m = mock[ItemIndexService]
      m.distinct(Matchers.eq("contributorDetails.contributor"), any[Seq[String]]) returns Future { Success(contributorValues) }
      m.distinct(Matchers.eq("taskInfo.gradeLevel"), any[Seq[String]]) returns Future { Success(gradeValues) }
      m
    }

    lazy val standardService = {
      val m = mock[StandardService]
      m
    }

    lazy val subjectService = {
      val m = mock[SubjectService]
      m
    }

    val fieldValuesApi = new FieldValuesApi(
      itemIndexService,
      v2ApiContext,
      standardService,
      subjectService,
      jsonFormatting,
      getOrgAndOptionsFn)
  }

  "contributors" should {

    "return 200" in new apiScope() {
      status(fieldValuesApi.contributors()(FakeRequest())) === OK
    }

    "return contributors from ItemIndexService" in new apiScope() {
      val json = contentAsJson(fieldValuesApi.contributors()(FakeRequest()))
      json.as[Seq[String]] === contributorValues
    }

    "user not authenticated" should {

      s"return ${testError.statusCode}" in new apiScope(orgAndOpts = Failure(testError)) {
        status(fieldValuesApi.contributors()(FakeRequest())) === testError.statusCode
      }
    }
  }

  "gradeLevels" should {

    "return 200" in new apiScope() {
      status(fieldValuesApi.gradeLevels()(FakeRequest())) === OK
    }

    "return gradeLevels from itemIndexService" in new apiScope() {
      val json = contentAsJson(fieldValuesApi.gradeLevels()(FakeRequest()))
      json.as[Seq[String]] === gradeValues
    }

    "user not authenticated" should {

      s"return ${testError.statusCode}" in new apiScope(orgAndOpts = Failure(testError)) {
        status(fieldValuesApi.gradeLevels()(FakeRequest())) === testError.statusCode
      }
    }
  }

}
