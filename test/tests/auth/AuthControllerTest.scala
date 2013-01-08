package tests.auth

import tests.BaseTest
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.Play.current
import play.api.Play
import securesocial.core.SecureSocial
import controllers.auth.{Permission, OAuthConstants}
import models.{User, Organization}
import play.api.libs.json.Json
import models.auth.{AccessToken, ApiClient}
import scala.Left
import scala.Right
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId

class AuthControllerTest extends BaseTest{

  "cannot register organization for client id and secret without logging in through SS" in {
    Organization.insert(new Organization("test"),None) match {
      case Right(org) => {
        val fakeRequest = FakeRequest(POST, "/auth/register").withFormUrlEncodedBody(OAuthConstants.Organization -> org.id.toString)
        val result = routeAndCall(fakeRequest).get
        Organization.delete(org.id)
        status(result) must not equalTo(OK)
      }
      case Left(error) => failure(error.message)
    }
  }

  "cannot register organization for client id and secret when logged in through SS without write permission to organization" in {
    Organization.insert(new Organization("test"),None) match {
      case Right(org) => User.insertUser(new User("testoplenty"),org.id,Permission.Read) match {
          case Right(user) => {
            val fakeRequest = FakeRequest(POST, "/auth/register").withSession(
              (SecureSocial.UserKey -> user.userName),
              (SecureSocial.ProviderKey -> "userpass")
            ).withFormUrlEncodedBody(OAuthConstants.Organization -> org.id.toString)
            val result = routeAndCall(fakeRequest).get
            User.removeUser(user.id)
            Organization.delete(org.id)
            status(result) must equalTo(UNAUTHORIZED)
          }
          case Left(error) => failure(error.message)
        }
      case Left(error) => failure(error.message)
    }
  }

  "can register organization for client id and secret when logged in through SS and with write permission to organization" in {
    Organization.insert(new Organization("test"),None) match {
      case Right(org) => User.insertUser(new User("testoplenty"),org.id,Permission.Write) match {
        case Right(user) => {
          val fakeRequest = FakeRequest(POST, "/auth/register").withSession(
            (SecureSocial.UserKey -> user.userName),
            (SecureSocial.ProviderKey -> "userpass")
          ).withFormUrlEncodedBody(OAuthConstants.Organization -> org.id.toString)
          val result = routeAndCall(fakeRequest).get
          val json = Json.parse(contentAsString(result))
          val clientId = (json \ OAuthConstants.ClientId).as[String]
          val clientSecret = (json \ OAuthConstants.ClientSecret).as[String]
          ApiClient.remove(MongoDBObject(ApiClient.clientId -> new ObjectId(clientId), ApiClient.clientSecret -> clientSecret))
          User.removeUser(user.id)
          Organization.delete(org.id)
          status(result) must equalTo(OK)
        }
        case Left(error) => failure(error.message)
      }
      case Left(error) => failure(error.message)
    }
  }

  "re-registering organization results in same client id and secret" in {
    Organization.insert(new Organization("test"),None) match {
      case Right(org) => User.insertUser(new User("testoplenty"),org.id,Permission.Write) match {
        case Right(user) => {
          val fakeRequest1 = FakeRequest(POST, "/auth/register").withSession(
            (SecureSocial.UserKey -> user.userName),
            (SecureSocial.ProviderKey -> "userpass")
          ).withFormUrlEncodedBody(OAuthConstants.Organization -> org.id.toString)
          val result1 = routeAndCall(fakeRequest1).get
          val json1 = Json.parse(contentAsString(result1))
          val clientId1 = (json1 \ OAuthConstants.ClientId).as[String]
          val clientSecret1 = (json1 \ OAuthConstants.ClientSecret).as[String]
          val fakeRequest2 = FakeRequest(POST, "/auth/register").withSession(
            (SecureSocial.UserKey -> user.userName),
            (SecureSocial.ProviderKey -> "userpass")
          ).withFormUrlEncodedBody(OAuthConstants.Organization -> org.id.toString)
          val result2 = routeAndCall(fakeRequest2).get
          val json2 = Json.parse(contentAsString(result2))
          val clientId2 = (json2 \ OAuthConstants.ClientId).as[String]
          val clientSecret2 = (json2 \ OAuthConstants.ClientSecret).as[String]
          ApiClient.remove(MongoDBObject(ApiClient.clientId -> new ObjectId(clientId1), ApiClient.clientSecret -> clientSecret1))
          User.removeUser(user.id)
          Organization.delete(org.id)
          status(result1) must equalTo(OK)
          status(result2) must equalTo(OK)
          clientId1 must equalTo(clientId2)
          clientSecret1 must equalTo(clientSecret2)
        }
        case Left(error) => failure(error.message)
      }
      case Left(error) => failure(error.message)
    }
  }

  "can retrieve auth token using client id and secret" in {
    Organization.insert(new Organization("test"),None) match {
      case Right(org) => User.insertUser(new User("testoplenty"),org.id,Permission.Write) match {
        case Right(user) => {
          val fakeRequest = FakeRequest(POST, "/auth/register").withSession(
            (SecureSocial.UserKey -> user.userName),
            (SecureSocial.ProviderKey -> "userpass")
          ).withFormUrlEncodedBody(OAuthConstants.Organization -> org.id.toString)
          val result = routeAndCall(fakeRequest).get
          User.removeUser(user.id)
          Organization.delete(org.id)
          val json = Json.parse(contentAsString(result))
          val clientId = (json \ OAuthConstants.ClientId).as[String]
          val clientSecret = (json \ OAuthConstants.ClientSecret).as[String]
          val tokenRequest = FakeRequest(POST, "/auth/access_token").withSession(
            (SecureSocial.UserKey -> user.userName),
            (SecureSocial.ProviderKey -> "userpass")
          ).withFormUrlEncodedBody(
            (OAuthConstants.GrantType -> OAuthConstants.ClientCredentials),
            (OAuthConstants.ClientId -> clientId),
            (OAuthConstants.ClientSecret -> clientSecret),
            (OAuthConstants.Scope -> user.userName)
          )
          val tokenResult = routeAndCall(tokenRequest).get
          val jstoken = Json.parse(contentAsString(tokenResult))
          val token = (jstoken \ OAuthConstants.AccessToken).as[String]
          AccessToken.removeToken(token)
          ApiClient.remove(MongoDBObject(ApiClient.clientId -> new ObjectId(clientId), ApiClient.clientSecret -> clientSecret))
          User.removeUser(user.id)
          Organization.delete(org.id)
          status(tokenResult) must equalTo(OK)
        }
        case Left(error) => failure(error.message)
      }
      case Left(error) => failure(error.message)
    }
  }

  "can use auth token to retrieve list of organizations" in {
    Organization.insert(new Organization("test"),None) match {
      case Right(org) => User.insertUser(new User("testoplenty"),org.id,Permission.Write) match {
        case Right(user) => {
          val fakeRequest = FakeRequest(POST, "/auth/register").withSession(
            (SecureSocial.UserKey -> user.userName),
            (SecureSocial.ProviderKey -> "userpass")
          ).withFormUrlEncodedBody(OAuthConstants.Organization -> org.id.toString)
          val result = routeAndCall(fakeRequest).get
          val json = Json.parse(contentAsString(result))
          val clientId = (json \ OAuthConstants.ClientId).as[String]
          val clientSecret = (json \ OAuthConstants.ClientSecret).as[String]
          val tokenRequest = FakeRequest(POST, "/auth/access_token").withSession(
            (SecureSocial.UserKey -> user.userName),
            (SecureSocial.ProviderKey -> "userpass")
          ).withFormUrlEncodedBody(
            (OAuthConstants.GrantType -> OAuthConstants.ClientCredentials),
            (OAuthConstants.ClientId -> clientId),
            (OAuthConstants.ClientSecret -> clientSecret),
            (OAuthConstants.Scope -> user.userName)
          )
          val tokenResult = routeAndCall(tokenRequest).get
          val jstoken = Json.parse(contentAsString(tokenResult))
          val token = (jstoken \ OAuthConstants.AccessToken).as[String]
          val orgRequest = FakeRequest(GET, "/api/v1/organizations?access_token=%s".format(token))
          val Some(orgresult) = routeAndCall(orgRequest)
          AccessToken.removeToken(token)
          ApiClient.remove(MongoDBObject(ApiClient.clientId -> new ObjectId(clientId), ApiClient.clientSecret -> clientSecret))
          User.removeUser(user.id)
          Organization.delete(org.id)
          status(orgresult) must equalTo(OK)
        }
        case Left(error) => failure(error.message)
      }
      case Left(error) => failure(error.message)
    }
  }
}
