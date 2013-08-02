package tests.basiclti.controllers

import tests.{PlaySingleton, BaseTest}
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import basiclti.models.{LtiQuestion, LtiQuiz}
import org.bson.types.ObjectId
import play.api.libs.json.Json._
import play.api.libs.json.JsValue
import play.api.mvc.{Request, Cookie, AnyContent, AnyContentAsJson}
import models.Organization
import models.auth.AccessToken
import models.itemSession.ItemSessionSettings
import basiclti.accessControl.auth.cookies.LtiCookieKeys
import player.accessControl.cookies.PlayerCookieKeys
import org.specs2.mutable.Specification
import org.corespring.platform.data.mongo.models.VersionedId

class LaunchConfigTest extends Specification{

  PlaySingleton.start()

  val Routes = basiclti.controllers.routes.LtiQuizzes
  val MockOrgId: ObjectId = new ObjectId("51114b307fc1eaa866444648")

  private def getOrg: Organization = Organization.findOneById(MockOrgId).get

  private def getMockConfig: LtiQuiz = {
    val c = new LtiQuiz(id = new ObjectId(),
      resourceLinkId = "some link id",
      question = LtiQuestion(None, ItemSessionSettings()),
      participants = Seq(),
      orgId = Some(getOrg.id))
    LtiQuiz.insert(c)
    c
  }

  private def get(quiz:LtiQuiz): LtiQuiz = {
    val action = basiclti.controllers.LtiQuizzes.get(quiz.id)
    val request = FakeRequest("ignore", "ignore")
    val result = action(addSessionInfo(quiz,request))
    val json = parse(contentAsString(result))
    json.as[LtiQuiz]
    //callAndReturnModel(addSessionInfo(quiz,request))
  }

  private def addSessionInfo[A](quiz: LtiQuiz, r: FakeRequest[A]): FakeRequest[A] = {
    r.withSession(
      (LtiCookieKeys.QUIZ_ID -> quiz.id.toString),
      (PlayerCookieKeys.ORG_ID -> quiz.orgId.get.toString)
    )
  }

  private def update(quiz: LtiQuiz): LtiQuiz = {
    val action = basiclti.controllers.LtiQuizzes.update(quiz.id)
    val jsValue = toJson(quiz)
    val request = FakeRequest("ignore", "ignore", FakeHeaders(), AnyContentAsJson(jsValue))
    val result = action(addSessionInfo(quiz,request))
    val json = parse(contentAsString(result))
    json.as[LtiQuiz]
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
