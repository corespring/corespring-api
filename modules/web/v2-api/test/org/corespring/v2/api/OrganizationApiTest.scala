package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.services.OrgCollectionService
import org.corespring.v2.auth.models.{ AuthMode, MockFactory }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext
import scalaz.Success

class OrganizationApiTest extends Specification with MockFactory with Mockito {

  trait scope extends Scope {

    val collectionId = ObjectId.get

    lazy val orgAndOptsResult = Success(mockOrgAndOpts(AuthMode.UserSession))

    val orgCollectionService = {
      val m = mock[OrgCollectionService]
      m
    }

    val v2ApiContext = V2ApiExecutionContext(ExecutionContext.Implicits.global)

    val getOrgAndOptsFn = (rh: RequestHeader) => {
      orgAndOptsResult
    }

    val api = new OrganizationApi(
      orgCollectionService,
      v2ApiContext,
      getOrgAndOptsFn)
  }

  "getOrgsWithSharedCollection" should {

    "return a list of orgs that have the collection shared with them" in new scope {
      val result = api.getOrgsWithSharedCollection(collectionId)(FakeRequest())
      status(result) must_== OK
    }
  }
}
