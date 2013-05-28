package tests.auth

import tests.BaseTest
import play.api.test.FakeRequest
import org.specs2.execute.{Result, Failure}
import play.api.test.Helpers._
import securesocial.core.SecureSocial
import controllers.auth.{Permission, OAuthConstants}
import models.{User, Organization}
import play.api.libs.json.Json
import models.auth.{AccessToken, ApiClient}
import scala.Left
import scala.Right
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.mvc.AnyContentAsFormUrlEncoded
import common.encryption.ShaHash

class AuthControllerTest extends BaseTest {

  val Routes = controllers.auth.routes.AuthController

  /** Execute a Specs method body once we successfully insert the org, also remove Org afterwards */
  def withOrg(org: Organization, fn: Organization => Result, maybeParentId: Option[ObjectId] = None): Result = {
    Organization.insert(org, maybeParentId) match {
      case Right(o) => {
        val result = fn(o)
        Organization.delete(o.id)
        result
      }
      case Left(error) => Failure(error.message)
    }
  }

  /** Execute a specs method body once we successfully insert a User, also remove user afterwards */
  def withUser(user: User, orgId: ObjectId, p: Permission, fn: User => Result): Result = {
    User.insertUser(user, orgId, p) match {
      case Right(u) => {
        val result = fn(u)
        User.removeUser(u.id)
        result
      }
      case Left(error) => Failure(error.message)
    }
  }

  /** Execute a specs method body once we register and have a client id and secret, tidy up after */
  def withRegistration(orgId: ObjectId, user: Option[User], fn: ((String, String) => Result)): Result = {
    routeAndCall(registerRequest(orgId, user)) match {
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
      .withSession(secureSocialSession(Some(user)): _*)
      .withFormUrlEncodedBody(tokenFormBody(clientId, secret, user.userName, grantType): _*)

    routeAndCall(tokenRequest) match {
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

  def testUser = new User("testoplenty")

  def testOrg = new Organization("test")

  def secureSocialSession(u: Option[User]): Array[(String, String)] = u match {
    case Some(user) => Array(
      (SecureSocial.UserKey -> user.userName),
      (SecureSocial.ProviderKey -> "userpass"),
      (SecureSocial.LastAccessKey -> DateTime.now().toString)
    )
    case _ => Array()
  }

  def tokenFormBody(id: String, secret: String, username: String, grantType: Option[String] = None): Array[(String, String)] = {
    val signature = ShaHash.sign(
      OAuthConstants.ClientCredentials+":"+id+":"+OAuthConstants.Sha1Hash+":"+username,
      secret
    )
    val base = Array(
      (OAuthConstants.ClientId -> id),
      (OAuthConstants.ClientSecret -> secret),
      (OAuthConstants.Scope -> username))
    base ++ grantType.map((OAuthConstants.GrantType -> _))
  }

  def registerRequest(orgId: ObjectId, maybeUser: Option[User] = None): FakeRequest[AnyContentAsFormUrlEncoded] = {
    FakeRequest(Routes.register().method, Routes.register().url)
      .withSession(secureSocialSession(maybeUser): _*)
      .withFormUrlEncodedBody(OAuthConstants.Organization -> orgId.toString)
  }

  def removeApiClient(result: play.api.mvc.Result) {
    val (id, secret) = idAndSecret(result)
    ApiClient.remove(MongoDBObject(ApiClient.clientId -> id, ApiClient.clientSecret -> secret))
  }

  def removeToken(result: play.api.mvc.Result) {
    val json = Json.parse(contentAsString(result))
    val token = (json \ OAuthConstants.AccessToken).as[String]
    AccessToken.removeToken(token)
  }

  def idAndSecret(result: play.api.mvc.Result): (String, String) = {
    val json = Json.parse(contentAsString(result))
    val id = (json \ OAuthConstants.ClientId).as[String]
    val secret = (json \ OAuthConstants.ClientSecret).as[String]
    (id, secret)
  }

  "cannot register without logging in through SS" in {
    withOrg(testOrg, {
      org =>
        routeAndCall(registerRequest(org.id)) match {
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
            routeAndCall(registerRequest(org.id, Some(user))) match {
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
            val result1 = routeAndCall(registerRequest(org.id, Some(user))).get
            val result2 = routeAndCall(registerRequest(org.id, Some(user))).get
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
                    val OrgRoutes = api.v1.routes.OrganizationApi
                    val call = OrgRoutes.list()
                    val orgRequest = FakeRequest(call.method, (call.url + "?access_token=%s").format(token))
                    routeAndCall(orgRequest) match {
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
