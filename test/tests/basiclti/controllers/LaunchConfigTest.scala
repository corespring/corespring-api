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
    val call = Routes.get(quiz.id)
    val request = FakeRequest(call.method, call.url)
    callAndReturnModel(addSessionInfo(quiz,request))
  }

  private def addSessionInfo[A](quiz: LtiQuiz, r: FakeRequest[A]): FakeRequest[A] = {
    r.withSession(
      (LtiCookieKeys.QUIZ_ID -> quiz.id.toString),
      (PlayerCookieKeys.ORG_ID -> quiz.orgId.get.toString)
    )
  }

  private def update(quiz: LtiQuiz): LtiQuiz = {
    val call = Routes.update(quiz.id)
    val jsValue = toJson(quiz)
    val request = FakeRequest(call.method, call.url, FakeHeaders(), AnyContentAsJson(jsValue))
    callAndReturnModel(addSessionInfo(quiz, request))
  }

  private def callAndReturnModel[T <: AnyContent](request: FakeRequest[T]): LtiQuiz = {
    routeAndCall(request) match {
      case Some(result) => {
        val resultString = contentAsString(result)
        println("resultString: ")
        println(resultString)
        val json: JsValue = parse(resultString)

        json.as[LtiQuiz]
      }
      case _ => throw new RuntimeException("couldn't get result")
    }
  }

  "launch config" should {

    "return a config" in {
      val c = getMockConfig
      val configFromController = get(c)
      configFromController.id === c.id
    }

    "update a config" in {
      val c = getMockConfig
      val copiedConfig = c.copy(question = LtiQuestion(Some(new ObjectId()), ItemSessionSettings()))
      update(copiedConfig).question.itemId === copiedConfig.question.itemId
    }

    "not allow an update if the user org doesn't match the db org" in {
      val c = getMockConfig
      val newOrg = new Organization(id = new ObjectId(), name = "some new org")
      val copiedConfig = c.copy(orgId = Some(newOrg.id), question = LtiQuestion(Some(new ObjectId()), ItemSessionSettings()))

      val call = Routes.update(copiedConfig.id)
      val jsValue = toJson(copiedConfig)
      val request = FakeRequest(call.method, call.url, FakeHeaders(), AnyContentAsJson(jsValue))

      routeAndCall(addSessionInfo(copiedConfig,request)) match {
        case Some(r) => {
          status(r) === BAD_REQUEST
        }
        case _ => failure("expected a bad request")
      }
    }
  }
}
