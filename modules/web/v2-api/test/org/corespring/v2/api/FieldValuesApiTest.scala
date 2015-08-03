package org.corespring.v2.api

import org.corespring.itemSearch.ItemIndexService
import org.corespring.test.PlaySingleton
import org.corespring.v2.auth.models.{ MockFactory, OrgAndOpts }
import org.corespring.v2.errors.Errors.invalidToken
import org.corespring.v2.errors.V2Error
import org.mockito.Matchers
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.{ Failure, Validation, Success }

class FieldValuesApiTest extends Specification with MockFactory {

  PlaySingleton.start()

  val contributorValues = Seq("these", "are", "contributors")
  val gradeValues = Seq("these are grades")

  class apiScope(orgAndOpts: Option[OrgAndOpts] = Some(mockOrgAndOpts())) extends Scope {
    val fieldValuesApi = new FieldValuesApi {

      def itemIndexService = {
        val m = mock[ItemIndexService]
        m.distinct(Matchers.eq("contributorDetails.contributor"), any[Seq[String]]) returns Future { Success(contributorValues) }
        m.distinct(Matchers.eq("taskInfo.gradeLevel"), any[Seq[String]]) returns Future { Success(gradeValues) }
        m
      }
      override implicit def ec: ExecutionContext = ExecutionContext.global
      override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = {
        orgAndOpts match {
          case Some(orgAndOpts) => Success(orgAndOpts)
          case _ => Failure(invalidToken(FakeRequest()))
        }
      }
    }
  }

  "contributors" should {

    "return 200" in new apiScope() {
      status(fieldValuesApi.contributors()(FakeRequest())) must be equalTo (OK)
    }

    "return contributors from ItemIndexService" in new apiScope() {
      val json = contentAsJson(fieldValuesApi.contributors()(FakeRequest()))
      json.as[Seq[String]] must be equalTo (contributorValues)
    }

    "user not authenticated" should {

      "return 401" in new apiScope(orgAndOpts = None) {
        status(fieldValuesApi.contributors()(FakeRequest())) must be equalTo (UNAUTHORIZED)
      }

    }

  }

  "gradeLevels" should {

    "return 200" in new apiScope() {
      status(fieldValuesApi.gradeLevels()(FakeRequest())) must be equalTo (OK)
    }

    "return gradeLevels from itemIndexService" in new apiScope() {
      val json = contentAsJson(fieldValuesApi.gradeLevels()(FakeRequest()))
      json.as[Seq[String]] must be equalTo (gradeValues)
    }

    "user not authenticated" should {

      "return 401" in new apiScope(orgAndOpts = None) {
        status(fieldValuesApi.gradeLevels()(FakeRequest())) must be equalTo (UNAUTHORIZED)
      }

    }

  }

}
