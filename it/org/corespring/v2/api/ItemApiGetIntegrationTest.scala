package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.it.helpers.{ StandardHelper, CollectionHelper, ItemHelper }
import org.corespring.it.scope.scopes.orgWithAccessToken
import org.corespring.models.Standard
import org.corespring.models.item._
import org.corespring.models.item.resource.{ Resource, VirtualFile }
import org.specs2.matcher.MatchResult
import play.api.http.Writeable
import play.api.libs.functional.syntax
import play.api.libs.json.{ Json, JsValue }
import play.api.mvc.{ AnyContent, AnyContentAsEmpty }
import play.api.test.{ FakeHeaders, FakeRequest }

class ItemApiGetIntegrationTest extends IntegrationSpecification {

  val routes = org.corespring.v2.api.routes.ItemApi

  "V2 - ItemApi" should {

    "get" should {

      trait itemApiTestOrgWithAccessTokenAndItem extends orgWithAccessToken {

        val collectionId = CollectionHelper.create(orgId)

        val qtiXmlTemplate = "<assessmentItem><itemBody>::version::</itemBody></assessmentItem>"
        val qti = VirtualFile("qti.xml", "text/xml", true, qtiXmlTemplate)
        val data: Resource = Resource(name = "data", files = Seq(qti))
        val item = new Item(
          data = Some(data),
          collectionId = collectionId.toString,
          contributorDetails = Some(new ContributorDetails(
            author = Some("Author"),
            contributor = Some("Contributor"),
            copyright = Some(new Copyright(
              owner = Some("Copyright Owner"))),
            credentials = Some("Test Item Writer"))),
          taskInfo = Some(new TaskInfo(
            title = Some("Title"),
            subjects = Some(new Subjects(
              primary = Some(new ObjectId("4ffb535f6bb41e469c0bf2aa")), //AP Art History
              related = Seq(new ObjectId("4ffb535f6bb41e469c0bf2ae")) //AP English Literature
              )),
            gradeLevel = Seq("GradeLevel1", "GradeLevel2"),
            itemType = Some("ItemType"))),
          standards = Seq("RL.1.5", "RI.5.8"),
          otherAlignments = Some(new Alignments(
            keySkills = Seq("KeySkill1", "KeySkill2"),
            bloomsTaxonomy = Some("BloomsTaxonomy"))),
          priorUse = Some("PriorUse"))
        val itemId = ItemHelper.create(collectionId, item)

        override def after: Any = {
          println("[orgWithAccessTokenAndItem] after")
          super.after
          CollectionHelper.delete(collectionId)
          ItemHelper.delete(itemId)
        }
      }

      def assertStatus[A](r: FakeRequest[A], expectedStatus: Int = OK)(implicit wr: Writeable[A]): MatchResult[Any] = {
        route(r).map { result =>
          status(result) === expectedStatus
        }.getOrElse(ko("no route found"))
      }

      def assertJson[A](r: FakeRequest[A], doAssert: (JsValue) => MatchResult[Any])(implicit wr: Writeable[A]): MatchResult[Any] = {
        route(r).map { result =>
          doAssert(contentAsJson(result))
        }.getOrElse(ko("no route found"))
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

        val mockStandards = Seq(Standard(Some("RL.1.5")), Standard(Some("RI.5.8")))

        override def after = {
          super.after
          StandardHelper.delete(mockStandards.map(_.id))
        }

        StandardHelper.create(mockStandards: _*)

        import play.api.libs.json._

        val json =
          s"""{
             |  "$$set": {
             |    "standards": [ ${mockStandards.map(s => s""" "${s.dotNotation.get}" """).mkString(",")}]
             |  }
             |}""".stripMargin
        ItemHelper.update(itemId, Json.parse(json))

        val r = createRequest[AnyContentAsEmpty.type](itemId.toString(), s"access_token=$accessToken", Some("application/json"), None)
        assertStatus(r, OK)

        assertJson(r, (jsonResult: JsValue) => {
          (jsonResult \ "id").asOpt[String] === Some(itemId.toString)
          (jsonResult \ "author").asOpt[String] === Some("Author")
          (jsonResult \ "title").asOpt[String] === Some("Title")
          (jsonResult \ "primarySubject" \ "subject").asOpt[String] === Some("AP Art History")
          (jsonResult \ "relatedSubject" \\ "subject").map(_.as[String]) === Seq("AP English Literature")
          (jsonResult \ "gradeLevel").as[Seq[String]] === Seq("GradeLevel1", "GradeLevel2")
          (jsonResult \ "itemType").asOpt[String] === Some("ItemType")
          val standards: Seq[JsValue] = (jsonResult \ "standards").as[Seq[JsValue]]
          (standards(0) \ "dotNotation").asOpt[String] === Some("RL.1.5")
          (standards(1) \ "dotNotation").asOpt[String] === Some("RI.5.8")
          (jsonResult \ "priorUse" \ "use").asOpt[String] === Some("PriorUse")
        })
      }
    }
  }

}
