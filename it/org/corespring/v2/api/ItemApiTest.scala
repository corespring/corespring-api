package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.platform.core.models.item.resource.{Resource, VirtualFile}
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.SecureSocialHelpers
import org.corespring.test.helpers.models.{CollectionHelper, ItemHelper}
import org.corespring.platform.core.models.item._
import org.corespring.v2.player.scopes.{ user, orgWithAccessToken }
import play.api.http.Writeable
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ AnyContent, AnyContentAsEmpty, AnyContentAsJson }
import play.api.test.{ FakeHeaders, FakeRequest }

trait itemApiTestOrgWithAccessTokenAndItem extends orgWithAccessToken {

  val collectionId = CollectionHelper.create(orgId)

  val qtiXmlTemplate = "<assessmentItem><itemBody>::version::</itemBody></assessmentItem>"
  val qti = VirtualFile("qti.xml", "text/xml", true, qtiXmlTemplate)
  val data: Resource = Resource(name = "data", files = Seq(qti))
  val item = new Item(
    data = Some(data),
    collectionId = Some(collectionId.toString),
    contributorDetails = Some(new ContributorDetails(
      author=Some("Author"),
      copyright = Some(new Copyright(
        owner=Some("Copyright Owner")
      )),
      credentials = Some("Test Item Writer")
    )),
    taskInfo = Some( new TaskInfo(
      title = Some("Title"),
      subjects = Some( new Subjects(
        primary = Some(new ObjectId("4ffb535f6bb41e469c0bf2aa")), //AP Art History
        related = Some(new ObjectId("4ffb535f6bb41e469c0bf2ae")) //AP English Literature
      )),
      gradeLevel = Seq("GradeLevel1", "GradeLevel2"),
      itemType = Some("ItemType")
    )),
    standards = Seq("RL.1.5", "RI.5.8"),
    otherAlignments = Some( new Alignments(
      keySkills = Seq("KeySkill1", "KeySkill2"),
      bloomsTaxonomy = Some("BloomsTaxonomy")
    )),
    priorUse = Some("PriorUse")
  )
  val itemId = ItemHelper.create(collectionId, item)

  override def after: Any = {
    println("[orgWithAccessTokenAndItem] after")
    super.after
    CollectionHelper.delete(collectionId)
    ItemHelper.delete(itemId)
  }
}

class ItemApiTest extends IntegrationSpecification {

  val create = org.corespring.v2.api.routes.ItemApi.create()

  "V2 - ItemApi" should {
    "create" should {

      def assertCall[A](r: FakeRequest[A], expectedStatus: Int = OK)(implicit wr: Writeable[A]) = {
        route(r).map { result =>

          if (status(result) == OK) {
            val id = (contentAsJson(result) \ "id").asOpt[String]
            ItemHelper.delete(VersionedId(id.get).get)
          }

          status(result) === expectedStatus
        }.getOrElse(failure("no route found"))
      }

      def createRequest[B <: AnyContent](query: String = "", contentTypeHeader: Option[String] = None, json: Option[JsValue] = None): FakeRequest[B] = {
        val r: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
          create.method,
          if (query.isEmpty) create.url else s"${create.url}?$query",
          FakeHeaders(),
          AnyContentAsEmpty)

        val withHeader: FakeRequest[B] = contentTypeHeader.map(ct => r.withHeaders(CONTENT_TYPE -> ct)).getOrElse(r).asInstanceOf[FakeRequest[B]]
        val out: FakeRequest[B] = json.map(j => withHeader.withJsonBody(j)).getOrElse(withHeader).asInstanceOf[FakeRequest[B]]
        out
      }

      s"$UNAUTHORIZED - for plain request" in {
        val r: FakeRequest[AnyContentAsEmpty.type] = createRequest[AnyContentAsEmpty.type]()
        assertCall(r, UNAUTHORIZED)
      }

      s"$BAD_REQUEST - for token based request with no json header" in new orgWithAccessToken {
        assertCall(createRequest[AnyContentAsEmpty.type](s"access_token=$accessToken"), BAD_REQUEST)
      }

      s"$BAD_REQUEST - for token based request with json header - but no json body" in new orgWithAccessToken {
        assertCall(
          createRequest[AnyContentAsEmpty.type](s"access_token=$accessToken", Some("application/json")),
          BAD_REQUEST)
      }

      s"$OK - for token based request with json header - with json body" in new orgWithAccessToken {
        assertCall(
          createRequest[AnyContentAsJson](s"access_token=$accessToken", Some("application/json"), Some(Json.obj())),
          OK)
      }

    }

    "get" should {

      def assertStatus[A](r: FakeRequest[A], expectedStatus: Int = OK)(implicit wr: Writeable[A]) = {
        route(r).map { result =>

         status(result) === expectedStatus

        }.getOrElse(failure("no route found"))
      }

      def assertJson[A](r: FakeRequest[A], doAssert: (JsValue)=>Unit)(implicit wr: Writeable[A]) = {
        route(r).map { result =>

         doAssert(contentAsJson(result))

        }.getOrElse(failure("no route found"))
      }

      def createRequest[B <: AnyContent](id: String, query: String = "", contentTypeHeader: Option[String] = None, json: Option[JsValue] = None): FakeRequest[B] = {
        val get = org.corespring.v2.api.routes.ItemApi.get(id)
        val r: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
          get.method,
          if (query.isEmpty) get.url else s"${get.url}?$query",
          FakeHeaders(),
          AnyContentAsEmpty)

        val withHeader: FakeRequest[B] = contentTypeHeader.map(ct => r.withHeaders(CONTENT_TYPE -> ct)).getOrElse(r).asInstanceOf[FakeRequest[B]]
        val out: FakeRequest[B] = json.map(j => withHeader.withJsonBody(j)).getOrElse(withHeader).asInstanceOf[FakeRequest[B]]
        out
      }

      s"$UNAUTHORIZED - for plain request" in {
        val r: FakeRequest[AnyContentAsEmpty.type] = createRequest[AnyContentAsEmpty.type](ObjectId.get.toString)
        assertStatus(r, UNAUTHORIZED)
      }

      s"$OK - for token based request with json header - with json body" in new itemApiTestOrgWithAccessTokenAndItem {
        val r = createRequest[AnyContentAsEmpty.type](itemId.toString(), s"access_token=$accessToken", Some("application/json"), None)
        assertStatus(r, OK)

        assertJson(r,(jsonResult: JsValue) => {
          (jsonResult \ "id").asOpt[String] === Some(itemId.toString)
          (jsonResult \ "author").asOpt[String] === Some("Author")
          (jsonResult \ "title").asOpt[String] === Some("Title")
          (jsonResult \ "primarySubject" \ "subject").asOpt[String] === Some("AP Art History")
          (jsonResult \ "relatedSubject" \ "subject").asOpt[String] === Some("AP English Literature")
          // detail only (jsonResult \ "copyrightOwner").asOpt[String] === Some("Copyright Owner")
          // detail only (jsonResult \ "credentials").asOpt[String] === Some("Test Item Writer")
          (jsonResult \ "gradeLevel").as[Seq[String]] === Seq("GradeLevel1", "GradeLevel2")
          (jsonResult \ "itemType").asOpt[String] === Some("ItemType")
          val standards: Seq[JsValue] = (jsonResult \ "standards").as[Seq[JsValue]]
          (standards(0) \ "dotNotation").asOpt[String] === Some("RL.1.5")
          (standards(1) \ "dotNotation").asOpt[String] === Some("RI.5.8")
          // detail only (jsonResult \ "keySkills").as[Seq[String]] === Seq("KeySkill1", "KeySkill2")
          // detail only (jsonResult \ "bloomsTaxonomy").asOpt[String] === Some("BloomsTaxonomy")
          (jsonResult \ "priorUse" \ "use").asOpt[String] === Some("PriorUse")
        })
      }
    }

    "getItemWithV1" should {

      def assertStatus[A](r: FakeRequest[A], expectedStatus: Int = OK)(implicit wr: Writeable[A]) = {
        route(r).map { result =>

          status(result) === expectedStatus

        }.getOrElse(failure("no route found"))
      }

      def createRequest[B <: AnyContent](id: VersionedId[ObjectId], query: String = "", contentTypeHeader: Option[String] = None, json: Option[JsValue] = None): FakeRequest[B] = {
        val getItemWithV1 = org.corespring.v2.api.routes.ItemApi.getItemWithV1(id, None)
        val r: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
          getItemWithV1.method,
          if (query.isEmpty) getItemWithV1.url else s"${getItemWithV1.url}?$query",
          FakeHeaders(),
          AnyContentAsEmpty)

        val withHeader: FakeRequest[B] = contentTypeHeader.map(ct => r.withHeaders(CONTENT_TYPE -> ct)).getOrElse(r).asInstanceOf[FakeRequest[B]]
        val out: FakeRequest[B] = json.map(j => withHeader.withJsonBody(j)).getOrElse(withHeader).asInstanceOf[FakeRequest[B]]
        out
      }

      "work" in new itemApiTestOrgWithAccessTokenAndItem {
        val r = createRequest[AnyContentAsEmpty.type](itemId, s"access_token=$accessToken", Some("application/json"), None)
        assertStatus(r, OK)
      }

    }
  }

}
