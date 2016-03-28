package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.models.{ColorPalette, ContentCollRef, Organization}
import org.corespring.models.auth.Permission
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.{OrganizationService, OrgCollectionService}
import org.corespring.v2.auth.models.{ AuthMode, MockFactory }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext
import scalaz.Success

class OrganizationApiTest extends Specification with MockFactory with Mockito {

  trait scope extends Scope {

    val collectionId = ObjectId.get

    val colorPalette =
      ColorPalette("#AAAAAA", "#BBBBBB", "#CCCCCC", "#DDDDDD", "#EEEEEE", "#111111", "#222222", "#333333", "#444444")

    lazy val mockedOrgAndOpts = mockOrgAndOpts(AuthMode.UserSession, colorPalette = colorPalette)
    lazy val orgAndOptsResult = Success(mockedOrgAndOpts)


    protected def mkOrg(id: ObjectId, p: Permission) = {
      Organization("test-org", contentcolls = Seq(ContentCollRef(id, p.value, true)), colorPalette = colorPalette)
    }

    lazy val orgCollectionService = {
      val m = mock[OrgCollectionService]
      m.ownsCollection(any[Organization], any[ObjectId]) returns Success(true)
      m.getOrgsWithAccessTo(any[ObjectId]) returns Stream.empty
      m
    }

    val orgService = mock[OrganizationService]

    val v2ApiContext = V2ApiExecutionContext(ExecutionContext.Implicits.global)

    val getOrgAndOptsFn = (rh: RequestHeader) => {
      orgAndOptsResult
    }

    val api = new OrganizationApi(
      orgService,
      orgCollectionService,
      v2ApiContext,
      getOrgAndOptsFn)
  }

  "getOrgsWithSharedCollection" should {

    "return an empty list of orgs that have the collection shared with them" in new scope {
      val result = api.getOrgsWithSharedCollection(collectionId)(FakeRequest())
      contentAsJson(result) must_== Json.arr()
    }

    trait withTwoOrgs extends scope {
      val orgWithWrite = mkOrg(collectionId, Permission.Write)
      orgCollectionService.getOrgsWithAccessTo(any[ObjectId]) returns Stream(orgWithWrite, mockedOrgAndOpts.org)
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

  "getColorPalette" should {

    implicit val reads = new ColorPalette.Reads(ColorPalette.default)

    "return colorPalette from current org" in new scope {
      val result = api.getColorPalette(FakeRequest())
      val palette = Json.fromJson[ColorPalette](contentAsJson(result))
        .getOrElse(throw new Exception("Could not deserialize result"))
      palette must be equalTo(colorPalette)
    }

  }

  "setColorPalette" should {

    implicit val writes = ColorPalette.Writes

    val updatedColorPalette =
      ColorPalette("#444444", "#333333", "#222222", "#111111", "#EEEEEE", "#DDDDDD", "#CCCCCC", "#BBBBBB", "#AAAAAA")
    val json = Json.toJson(updatedColorPalette)

    "return updated colorPalette" in new scope {
      orgService.save(any[Organization]) answers { (obj, mock) =>
        val arr = obj.asInstanceOf[Array[Any]]
        val d = arr(0).asInstanceOf[Organization]
        Success(d)
      }
      val result = api.updateColorPalette(FakeRequest().withJsonBody(json))
      contentAsJson(result) must be equalTo(json)
    }

  }

}
