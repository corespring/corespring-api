package tests.basiclti.controllers

import tests.BaseTest
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import basiclti.models.{LtiQuestion, LtiQuiz}
import org.bson.types.ObjectId
import play.api.libs.json.Json._
import play.api.libs.json.JsValue
import play.api.mvc.{AnyContent, AnyContentAsJson}
import models.Organization
import models.auth.AccessToken
import models.itemSession.ItemSessionSettings

class LaunchConfigTest extends BaseTest {

  val Routes = basiclti.controllers.routes.LtiQuizzes
  val MockOrgId : ObjectId = new ObjectId( "51114b307fc1eaa866444648" )

  private def getOrg : Organization = Organization.findOneById(MockOrgId).get

  private def getMockConfig : LtiQuiz = {
    val c = new LtiQuiz(id = new ObjectId(),
      resourceLinkId = "some link id",
      question = LtiQuestion(None, ItemSessionSettings()),
      participants = Seq(),
      orgId = Some(getOrg.id))
    LtiQuiz.insert(c)
    c
  }

  private def get(id:ObjectId) : LtiQuiz = {
    val call = Routes.get(id)
    val request = FakeRequest(call.method, tokenize(call.url))
    callAndReturnModel(request)
  }

  private def update(config:LtiQuiz) : LtiQuiz = {
    val call = Routes.update(config.id)
    val jsValue = toJson(config)
    val request = FakeRequest(call.method, tokenize(call.url), FakeHeaders(), AnyContentAsJson(jsValue))
    callAndReturnModel(request)
  }

  private def callAndReturnModel[T <: AnyContent](request : FakeRequest[T]) : LtiQuiz = {
    routeAndCall(request) match {
      case Some(result) => {
        val resultString = contentAsString(result)
        println("resultString: ")
        println(resultString)
        val json : JsValue = parse(resultString)

        json.as[LtiQuiz]
      }
      case _ => throw new RuntimeException("couldn't get result")
    }
  }

  "launch config" should {

    "return a config" in {
      val c = getMockConfig
      val configFromController = get(c.id)
      configFromController.id === c.id
    }

    "update a config" in {
      val c = getMockConfig
      val copiedConfig = c.copy( question = LtiQuestion(Some(new ObjectId()), ItemSessionSettings()))
      update(copiedConfig).question.itemId === copiedConfig.question.itemId
    }

    def getAccessTokenForOrg(org:Organization) : AccessToken = AccessToken.getTokenForOrg(org)

    "not allow an update if the user org doesn't match the db org" in {
      val c = getMockConfig
      val copiedConfig = c.copy( question = LtiQuestion(Some(new ObjectId()), ItemSessionSettings()) )

      val newOrg = new Organization(id = new ObjectId(), name = "some new org")
      val token : AccessToken = getAccessTokenForOrg(newOrg)
      val call = Routes.update(copiedConfig.id)
      val jsValue = toJson(copiedConfig)
      val url = tokenize(call.url, token.tokenId)
      val request = FakeRequest(call.method, url, FakeHeaders(), AnyContentAsJson(jsValue))

      routeAndCall(request) match {
        case Some(r) => {
          status(r) === BAD_REQUEST
        }
        case _ => failure("expected a bad request")
      }
    }
  }
}
