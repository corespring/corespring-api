package api.v1

import models.metadata.MetadataSetServiceImpl
import models.{SchemaMetadata, MetadataSet}
import org.bson.types.ObjectId
import org.mockito.ArgumentMatcher
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AnyContentAsJson, AnyContent, AnyContentAsEmpty}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, FakeHeaders}
import tests.PlaySingleton

class MetadataSetApiTest extends Specification with Mockito {

  PlaySingleton.start()

  lazy val testOrgId = new ObjectId("51114b307fc1eaa866444648")

  class ObjectIdMatcher(val oid: ObjectId) extends ArgumentMatcher {
    def matches(p1: Any): Boolean = p1.isInstanceOf[ObjectId] && p1.asInstanceOf[ObjectId].toString == oid.toString
  }

  import org.mockito.Matchers.{anyObject, argThat => mockitoArgThat}

  def thisId(id: ObjectId) = mockitoArgThat(new ObjectIdMatcher(id))

  val dummySet = set("dummy")

  def set(k: String) = MetadataSet(s"${k}_key", s"${k}_url", s"${k}_label", true, Seq(SchemaMetadata(k)), ObjectId.get)

  def mockServiceImpl: MetadataSetServiceImpl = {
    val m = mock[MetadataSetServiceImpl]
    m.list(mockitoArgThat(new ObjectIdMatcher(testOrgId))) returns Seq(dummySet)
    m.create(mockitoArgThat(new ObjectIdMatcher(testOrgId)), anyObject()) returns Right(dummySet)
    m
  }

  def api(s: MetadataSetServiceImpl) = new MetadataSetApi(s)

  def req(content: AnyContent = AnyContentAsEmpty, token: String = "test_token") = FakeRequest("", s"?access_token=$token", FakeHeaders(), content)

  "api" should {

    "list" in {
      val service = mockServiceImpl
      val result = api(service).list(req())
      there was one(service).list(testOrgId)
      val json = Json.parse(contentAsString(result))
      val set: Seq[MetadataSet] = json.as[Seq[MetadataSet]]
      set.length === 1
    }

    def runCreate(requestContent: AnyContent, expectedStatus: Int = OK) = {
      val service = mockServiceImpl

      if (requestContent.isInstanceOf[AnyContentAsJson]) {
        val json = requestContent.asInstanceOf[AnyContentAsJson].json
        json.asOpt[MetadataSet].map {
          ms =>
            service.create(mockitoArgThat(new ObjectIdMatcher(testOrgId)), anyObject()) returns Right(ms)
        }
      }
      val result = api(service).create(req(content = requestContent))
      status(result) === expectedStatus
    }

    implicit def setToAnyContentAsJson(ms: MetadataSet): AnyContent = AnyContentAsJson(Json.toJson(ms))

    "create" in runCreate(set("create"), OK)
    "error in create if no json" in runCreate(AnyContentAsEmpty, BAD_REQUEST)
    "error in create if not useful json" in runCreate(AnyContentAsJson(JsObject(Seq())), BAD_REQUEST)


    "get returns ok" in {
      val service = mockServiceImpl
      val data = set("get_dummy")
      service.findOneById(anyObject()) returns Some(data)
      val result = api(service).get(ObjectId.get)(req())
      status(result) === OK
      Json.parse(contentAsString(result)).as[MetadataSet] === data
    }

    "get returns not found if it can't find the set" in {
      val service = mockServiceImpl
      service.findOneById(anyObject()) returns None
      val result = api(service).get(ObjectId.get)(req())
      status(result) === NOT_FOUND
    }

  }
}
