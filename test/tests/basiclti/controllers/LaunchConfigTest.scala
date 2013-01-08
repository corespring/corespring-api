package tests.basiclti.controllers

import org.specs2.mutable.Specification
import org.specs2.execute.Pending
import tests.{BaseTest, PlaySingleton}
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import basiclti.models.LtiLaunchConfiguration
import org.bson.types.ObjectId
import play.api.libs.json.Json._
import play.api.libs.json.JsValue
import play.api.mvc.{AnyContent, AnyContentAsJson, Result}
import models.Organization
import models.auth.{ApiClient, AccessToken}

class LaunchConfigTest extends BaseTest {

  val Routes = basiclti.controllers.routes.LaunchConfig
  val MockOrgId : ObjectId = new ObjectId( "502404dd0364dc35bb393397" )

  private def getOrg : Organization = Organization.findOneById(MockOrgId).get

  private def getMockConfig : LtiLaunchConfiguration = {
    val c = new LtiLaunchConfiguration(id = new ObjectId(),
      resourceLinkId = "some link id",
      itemId = None,
      sessionSettings = None,
      orgId = Some(getOrg.id))
    LtiLaunchConfiguration.insert(c)
    c
  }

  private def get(id:ObjectId) : LtiLaunchConfiguration = {
    val call = Routes.get(id)
    val request = FakeRequest(call.method, tokenize(call.url))
    callAndReturnModel(request)
  }

  private def update(config:LtiLaunchConfiguration) : LtiLaunchConfiguration = {
    val call = Routes.update(config.id)
    val jsValue = toJson(config)
    val request = FakeRequest(call.method, tokenize(call.url), FakeHeaders(), AnyContentAsJson(jsValue))
    callAndReturnModel(request)
  }

  private def callAndReturnModel[T <: AnyContent](request : FakeRequest[T]) : LtiLaunchConfiguration = {
    routeAndCall(request) match {
      case Some(result) => {
        val resultString = contentAsString(result)
        println("resultString: ")
        println(resultString)
        val json : JsValue = parse(resultString)

        json.as[LtiLaunchConfiguration]
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
      val copiedConfig = c.copy( itemId = Some(new ObjectId()))
      update(copiedConfig).itemId === copiedConfig.itemId
    }

    def getAccessTokenForOrg(org:Organization) : AccessToken = AccessToken.getTokenForOrg(org)

    "not allow an update if the user org doesn't match the db org" in {
      val c = getMockConfig
      val copiedConfig = c.copy( itemId = Some(new ObjectId()) )

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
