package tests.auth

import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.api.v1.routes.OrganizationApi
import org.corespring.platform.core.controllers.auth.OAuthConstants
import org.joda.time.DateTime
import play.api.mvc.{SimpleResult, AnyContentAsFormUrlEncoded}
import org.corespring.platform.core.models.{User, Organization}
import org.corespring.platform.core.models.auth.{Permission, ApiClient, AccessToken}
import org.corespring.platform.core.models.{User, Organization}
import org.corespring.test.{TestModelHelpers, BaseTest}
import org.specs2.execute.Result
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future

//TODO: Move AuthController to its own module then this test with it.
class AuthControllerTest extends BaseTest with TestModelHelpers {

  val Routes = org.corespring.platform.core.controllers.auth.routes.AuthController


  /** Execute a specs method body once we register and have a client id and secret, tidy up after */
  def withRegistration(orgId: ObjectId, user: Option[User], fn: ((String, String) => Result)): Result = {
    route(registerRequest(orgId, user)) match {
      case Some(result) => {
        if (status(result) == OK) {
          val (id, secret) = idAndSecret(result)
          val out = fn(id, secret)
          removeApiClient(result)
          out
        } else {
          failure("register failed for orgId: %s and user: %s".format(orgId.toString, user.toString))
        }
      }
      case _ => failure
    }
  }

  /** Execute a specs method body once we have a token and tidy up after */
  def withToken(user: User, clientId: String, secret: String, fn: (String => Result), grantType: Option[String] = None): Result = {
    val tokenRequest = FakeRequest(Routes.getAccessToken().method, Routes.getAccessToken().url)
      .withCookies(secureSocialCookie(Some(user)).toList : _*)
      .withFormUrlEncodedBody(tokenFormBody(clientId, secret, user.userName, grantType): _*)

    route(tokenRequest) match {
      case Some(result) => {
        val json = Json.parse(contentAsString(result))
        val token = (json \ OAuthConstants.AccessToken).as[String]
        val out = fn(token)
        removeToken(result)
        out
      }
      case _ => failure("with token failed")
    }
  }


  def registerRequest(orgId: ObjectId, maybeUser: Option[User] = None): FakeRequest[AnyContentAsFormUrlEncoded] = {
    FakeRequest(Routes.register().method, Routes.register().url)
      .withCookies(secureSocialCookie(maybeUser).toList : _*)
      .withFormUrlEncodedBody(OAuthConstants.Organization -> orgId.toString)
  }

  def removeApiClient(result: Future[SimpleResult]) {
    val (id, secret) = idAndSecret(result)
    ApiClient.remove(MongoDBObject(ApiClient.clientId -> id, ApiClient.clientSecret -> secret))
  }

  def removeToken(result: Future[SimpleResult]) {
    val json = Json.parse(contentAsString(result))
    val token = (json \ OAuthConstants.AccessToken).as[String]
    AccessToken.removeToken(token)
  }

  def idAndSecret(result: Future[SimpleResult]): (String, String) = {
    val json = Json.parse(contentAsString(result))
    val id = (json \ OAuthConstants.ClientId).as[String]
    val secret = (json \ OAuthConstants.ClientSecret).as[String]
    (id, secret)
  }

  "cannot register without logging in through SS" in {
    withOrg(testOrg, {
      org =>
        route(registerRequest(org.id)) match {
          case Some(result) => status(result) !== OK
          case _ => failure
        }
    })
  }

  "cannot register without write permission to organization" in {
    val resultFn = {
      org: Organization =>
        withUser(testUser, org.id, Permission.Read, {
          user =>
            route(registerRequest(org.id, Some(user))) match {
              case Some(result) => status(result) === UNAUTHORIZED
              case _ => failure
            }
        })
    }
    withOrg(testOrg, resultFn)
  }

  "can register with write permission to organization" in {
    val resultFn = {
      org: Organization =>
        withUser(testUser, org.id, Permission.Write, {
          user =>
            withRegistration(org.id, Some(user), {
              (id, secret) => success
            })
        })
    }
    withOrg(testOrg, resultFn)
  }

  "re-registering returns same client id and secret" in {
    val resultFn = {
      org: Organization =>
        withUser(testUser, org.id, Permission.Write, {
          user =>

            val result1 = route(registerRequest(org.id, Some(user))).get
            Thread.sleep(100)
            val result2 = route(registerRequest(org.id, Some(user))).get
            removeApiClient(result1)
            removeApiClient(result2)
            contentAsString(result1) === contentAsString(result2)
        })
    }
    withOrg(testOrg, resultFn)
  }

  def registerAndGetAccessTokenAndAssertStatus(grantType: Option[String] = None): (Organization => Result) = {
    org: Organization =>
      withUser(testUser, org.id, Permission.Write, {
        user =>
          withRegistration(org.id, Some(user), {
            (id, secret) =>
              withToken(user, id, secret, {
                token => success
              }, grantType)
          })
      })
  }

  "can retrieve auth token using client id and secret" in {
    withOrg(testOrg, registerAndGetAccessTokenAndAssertStatus(Some(OAuthConstants.ClientCredentials)))
  }

  "can retrieve auth token using client id and secret without sending grant type" in {
    withOrg(testOrg, registerAndGetAccessTokenAndAssertStatus())
  }

  "can use auth token to retrieve list of organizations" in {
    val resultFn = {
      org: Organization =>
        withUser(testUser, org.id, Permission.Write, {
          user =>
            withRegistration(org.id, Some(user), {
              (id, secret) =>
                withToken(user, id, secret, {
                  token =>
                    val OrgRoutes = OrganizationApi
                    val call = OrgRoutes.list()
                    val orgRequest = FakeRequest(call.method, (call.url + "?access_token=%s").format(token))
                    route(orgRequest) match {
                      case Some(result) => status(result) === OK
                      case _ => failure
                    }
                })
            })
        })
    }
    withOrg(testOrg, resultFn)
  }
}
