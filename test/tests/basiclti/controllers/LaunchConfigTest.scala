package tests.basiclti.controllers

import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import basiclti.models.{LtiQuestion, LtiAssessment}
import org.bson.types.ObjectId
import play.api.libs.json.Json._
import play.api.libs.json.JsValue
import play.api.mvc.{Request, Cookie, AnyContent, AnyContentAsJson}
import basiclti.accessControl.auth.cookies.LtiCookieKeys
import org.specs2.mutable.Specification
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.auth.AccessToken
import org.corespring.platform.core.models.itemSession.ItemSessionSettings
import org.corespring.test.PlaySingleton
import org.corespring.player.accessControl.cookies.PlayerCookieKeys

class LaunchConfigTest extends Specification{

  PlaySingleton.start()

  val Routes = basiclti.controllers.routes.LtiAssessments
  val MockOrgId: ObjectId = new ObjectId("51114b307fc1eaa866444648")

  private def getOrg: Organization = Organization.findOneById(MockOrgId).get

  private def getMockConfig: LtiAssessment = {
    val c = new LtiAssessment(id = new ObjectId(),
      resourceLinkId = "some link id",
      question = LtiQuestion(None, ItemSessionSettings()),
      participants = Seq(),
      orgId = Some(getOrg.id))
    LtiAssessment.insert(c)
    c
  }

  private def get(assessment:LtiAssessment): LtiAssessment = {
    val action = basiclti.controllers.LtiAssessments.get(assessment.id)
    val request = FakeRequest("ignore", "ignore")
    val result = action(addSessionInfo(assessment,request))
    val json = parse(contentAsString(result))
    json.as[LtiAssessment]
    //callAndReturnModel(addSessionInfo(assessment,request))
  }

  private def addSessionInfo[A](assessment: LtiAssessment, r: FakeRequest[A]): FakeRequest[A] = {
    r.withSession(
      (LtiCookieKeys.ASSESSMENT_ID -> assessment.id.toString),
      (PlayerCookieKeys.ORG_ID -> assessment.orgId.get.toString)
    )
  }

  private def update(assessment: LtiAssessment): LtiAssessment = {
    val action = basiclti.controllers.LtiAssessments.update(assessment.id)
    val jsValue = toJson(assessment)
    val request = FakeRequest("ignore", "ignore", FakeHeaders(), AnyContentAsJson(jsValue))
    val result = action(addSessionInfo(assessment,request))
    val json = parse(contentAsString(result))
    json.as[LtiAssessment]
  }

  "launch config" should {

    "return a config" in {
      val c = getMockConfig
      val configFromController = get(c)
      configFromController.id === c.id
    }

    "update a config" in {
      val c = getMockConfig
      val copiedConfig = c.copy(question = LtiQuestion(Some(VersionedId(new ObjectId())), ItemSessionSettings()))
      update(copiedConfig).question.itemId === copiedConfig.question.itemId
    }

    "not allow an update if the user org doesn't match the db org" in {
      val c = getMockConfig
      val newOrg = new Organization(id = new ObjectId(), name = "some new org")
      val copiedConfig = c.copy(orgId = Some(newOrg.id), question = LtiQuestion(Some(VersionedId(new ObjectId())), ItemSessionSettings()))

      val call = Routes.update(copiedConfig.id)
      val jsValue = toJson(copiedConfig)
      val request = FakeRequest(call.method, call.url, FakeHeaders(), AnyContentAsJson(jsValue))

      route(addSessionInfo(copiedConfig,request)) match {
        case Some(r) => {
          status(r) === BAD_REQUEST
        }
        case _ => failure("expected a bad request")
      }
    }
  }
}
