package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.{ ContentCollRef, Organization }
import org.corespring.services.OrgCollectionService
import org.corespring.v2.actions.V2ActionsFactory
import org.corespring.v2.auth.models.MockFactory
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsValue, Json }
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext
import scalaz.Success

class OrganizationApiTest extends Specification with MockFactory with Mockito {

  trait scope extends Scope {

    val collectionId = ObjectId.get

    protected def mkOrg(id: ObjectId, p: Permission) = {
      Organization("test-org", contentcolls = Seq(ContentCollRef(id, p.value, true)))
    }

    lazy val orgCollectionService = {
      val m = mock[OrgCollectionService]
      m.ownsCollection(any[Organization], any[ObjectId]) returns Success(true)
      m.getOrgsWithAccessTo(any[ObjectId]) returns Stream.empty
      m
    }

    val v2ApiContext = V2ApiExecutionContext(ExecutionContext.Implicits.global)

    val api = new OrganizationApi(
      V2ActionsFactory.apply,
      orgCollectionService,
      v2ApiContext)

  }

  "getOrgsWithSharedCollection" should {

    "return an empty list of orgs that have the collection shared with them" in new scope {
      val result = api.getOrgsWithSharedCollection(collectionId)(FakeRequest())
      contentAsJson(result) must_== Json.arr()
    }

    trait withTwoOrgs extends scope {
      val orgWithWrite = mkOrg(collectionId, Permission.Write)
      orgCollectionService.getOrgsWithAccessTo(any[ObjectId]) returns Stream(orgWithWrite, V2ActionsFactory.orgAndOpts.org)
      val result = api.getOrgsWithSharedCollection(collectionId)(FakeRequest())
      val json = contentAsJson(result)
    }

    "not return the org that made the request" in new withTwoOrgs {
      json.as[Seq[JsValue]].size must_== 1
    }

    "return json listing 1 org that has Write permission" in new withTwoOrgs {
      json must_== Json.arr(
        Json.obj(
          "name" -> orgWithWrite.name,
          "id" -> orgWithWrite.id.toString,
          "permission" -> Permission.Write.name))
    }
  }
}
