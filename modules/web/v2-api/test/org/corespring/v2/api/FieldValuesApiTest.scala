package org.corespring.v2.api

import org.corespring.platform.core.services.item.ItemIndexService
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

  val contributors = Seq("these", "are", "contributors")
  val grades = Seq("these are grades")

  class apiScope(orgAndOpts: Option[OrgAndOpts] = Some(mockOrgAndOpts())) extends Scope {
    val fieldValuesApi = new FieldValuesApi {

      def itemIndexService = {
        val m = mock[ItemIndexService]
        m.distinct(Matchers.eq("contributorDetails.contributor"), any[Seq[String]]) returns Future { Success(contributors) }
        m.distinct(Matchers.eq("taskInfo.gradeLevel"), any[Seq[String]]) returns Future { Success(grades) }
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

  "get" should {

    "return 200" in new apiScope() {
      status(fieldValuesApi.get("contributorDetails.contributor")(FakeRequest())) must be equalTo (OK)
    }

    "return contributors from ItemIndexService" in new apiScope() {
      val json = contentAsJson(fieldValuesApi.get("contributorDetails.contributor")(FakeRequest()))
      json.as[Seq[String]] must be equalTo (contributors)
    }

    "return gradeLevels from itemIndexService" in new apiScope() {
      val json = contentAsJson(fieldValuesApi.get("taskInfo.gradeLevel")(FakeRequest()))
      json.as[Seq[String]] must be equalTo (grades)
    }

    "user not authenticated" should {

      "return 401" in new apiScope(orgAndOpts = None) {
        status(fieldValuesApi.get("contributorDetails.contributor")(FakeRequest())) must be equalTo (UNAUTHORIZED)
      }

    }

  }

}
