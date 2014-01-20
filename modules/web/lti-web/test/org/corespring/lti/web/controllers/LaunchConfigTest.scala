package org.corespring.lti.web.controllers

import org.bson.types.ObjectId
import org.corespring.lti.models.{LtiQuestion, LtiQuiz}
import org.corespring.lti.web.accessControl.cookies.LtiCookieKeys
import org.corespring.lti.web.controllers
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.itemSession.ItemSessionSettings
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.player.accessControl.cookies.PlayerCookieKeys
import org.corespring.test.PlaySingleton
import org.specs2.mutable.Specification
import play.api.libs.json.Json._
import play.api.mvc.AnyContentAsJson
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}

class LaunchConfigTest extends Specification{

  PlaySingleton.start()

  val Routes = org.corespring.lti.web.controllers.routes.LtiQuizzes
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
    val action = LtiQuizzes.get(quiz.id)
    val request = FakeRequest("ignore", "ignore")
    val result = action(addSessionInfo(quiz,request))
    val json = parse(contentAsString(result))
    json.as[LtiQuiz]
    //callAndReturnModel(addSessionInfo(quiz,request))
  }

  private def addSessionInfo[A](quiz: LtiQuiz, r: FakeRequest[A]): FakeRequest[A] = {
    r.withSession(
      (LtiCookieKeys.QUIZ_ID -> quiz.id.toString),
      (PlayerCookieKeys.orgId -> quiz.orgId.get.toString)
    )
  }

  private def update(quiz: LtiQuiz): LtiQuiz = {
    val action = controllers.LtiQuizzes.update(quiz.id)
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
      val jsValue = toJson(copiedConfig)
      val request = FakeRequest("", "", FakeHeaders(), AnyContentAsJson(jsValue))
      val requestWithSession = addSessionInfo(copiedConfig, request)
      val result = LtiQuizzes.update(copiedConfig.id)(requestWithSession)
      status(result) === BAD_REQUEST
    }
  }
}
