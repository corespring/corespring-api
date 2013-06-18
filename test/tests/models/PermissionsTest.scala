package tests.models

import tests.BaseTest
import models._
import controllers.auth.Permission
import models.auth.AccessToken
import play.api.test.{FakeRequest}
import play.api.test.Helpers._
import play.api.libs.json.Json
import org.bson.types.ObjectId
import scala.Left
import play.api.test.FakeHeaders
import play.api.mvc.AnyContentAsJson
import scala.Right
import scala.Some
import com.mongodb.casbah.commons.MongoDBObject
import models.item.{Content, Item}

class PermissionsTest extends BaseTest{

  "cannot read an organization with no permission" in {
    Organization.insert(new Organization("test",Seq(),Seq()),None) match {
      case Right(org) => User.insertUser(new User("testoplenty","Test O'Plenty","testoplenty@gmail.com",None,"abc123"),org.id,Permission.None) match {
        case Right(user) => AccessToken.insertToken(new AccessToken(org.id,Some(user.userName),"testoplenty_token")) match {
          case Right(token) => {
            val fakeRequest = FakeRequest(GET, "/api/v1/organizations/"+org.id.toString+"?access_token="+token.tokenId)
            val Some(result) = routeAndCall(fakeRequest)
            AccessToken.remove(token)
            User.remove(user)
            Organization.remove(org)
            status(result) must beEqualTo(UNAUTHORIZED)
          }
          case Left(error) => failure(error.message)
        }
        case Left(error) => failure(error.message)
      }
      case Left(error) => failure(error.message)
    }
  }

  "can read an organization with read permission" in {
    Organization.insert(new Organization("test",Seq(),Seq()),None) match {
      case Right(org) => User.insertUser(new User("testoplenty","Test O'Plenty","testoplenty@gmail.com",Seq(),"abc123"),org.id,Permission.Read) match {
        case Right(user) => AccessToken.insertToken(new AccessToken(org.id,Some(user.userName),"testoplenty_token")) match {
          case Right(token) => {
            val fakeRequest = FakeRequest(GET, "/api/v1/organizations/"+org.id+"?access_token="+token.tokenId)
            val Some(result) = routeAndCall(fakeRequest)
            AccessToken.remove(token)
            User.remove(user)
            Organization.remove(org)
            status(result) must beEqualTo(OK)
            (Json.parse(contentAsString(result)) \ Organization.name).as[String] must equalTo(org.name)
          }
          case Left(error) => failure(error.message)
        }
        case Left(error) => failure(error.message)
      }
      case Left(error) => failure(error.message)
    }
  }

  "cannot create a collection in an organization with read permission"  in {
    Organization.insert(new Organization("test",Seq(),Seq()),None) match {
      case Right(org) => User.insertUser(new User("testoplenty","Test O'Plenty","testoplenty@gmail.com",Seq(),"abc123"),org.id,Permission.Read) match {
        case Right(user) => AccessToken.insertToken(new AccessToken(org.id,Some(user.userName),"testoplenty_token")) match {
          case Right(token) => {
            val fakeRequest = FakeRequest(POST, "/api/v1/collections?access_token="+token.tokenId, FakeHeaders(), AnyContentAsJson(Json.toJson(new ContentCollection("test"))))
            val result = routeAndCall(fakeRequest).get
            AccessToken.remove(token)
            User.remove(user)
            Organization.remove(org)
            status(result) must beEqualTo(UNAUTHORIZED)
          }
          case Left(error) => failure(error.message)
        }
        case Left(error) => failure(error.message)
      }
      case Left(error) => failure(error.message)
    }
  }

  "can create a collection in an organization with write permission" in {
    Organization.insert(new Organization("test",Seq(),Seq()),None) match {
      case Right(org) => User.insertUser(new User("testoplenty","Test O'Plenty","testoplenty@gmail.com",Seq(),"abc123"),org.id,Permission.Write) match {
        case Right(user) => AccessToken.insertToken(new AccessToken(org.id,Some(user.userName),"testoplenty_token")) match {
          case Right(token) => {
            val fakeRequest = FakeRequest(POST, "/api/v1/collections?access_token="+token.tokenId, FakeHeaders(), AnyContentAsJson(Json.toJson(Map("name" -> "test"))))
            val result = routeAndCall(fakeRequest).get
            val json = Json.parse(contentAsString(result))
            val id = new ObjectId((json \ "id").as[String])
            ContentCollection.removeById(id)
            AccessToken.remove(token)
            User.remove(user)
            Organization.remove(org)
            status(result) must beEqualTo(OK)
            (json \ ContentCollection.name).as[String] must beEqualTo("test")
          }
          case Left(error) => failure(error.message)
        }
        case Left(error) => failure(error.message)
      }
      case Left(error) => failure(error.message)
    }
  }

  "cannot read a collection of items with no permission" in {
    Organization.insert(new Organization("test",Seq(),Seq()),None) match {
      case Right(org) => User.insertUser(new User("testoplenty","Test O'Plenty","testoplenty@gmail.com",Seq(),"abc123"),org.id,Permission.Read) match {
        case Right(user) => AccessToken.insertToken(new AccessToken(org.id,Some(user.userName),"testoplenty_token")) match {
          case Right(token) => ContentCollection.insertCollection(org.id, new ContentCollection("test"), Permission.None) match {
            case Right(coll) => {
              val fakeRequest = FakeRequest(GET, "/api/v1/collections/"+coll.id.toString+"/items?access_token="+token.tokenId)
              val result = routeAndCall(fakeRequest).get
              ContentCollection.remove(coll)
              AccessToken.remove(token)
              User.remove(user)
              Organization.remove(org)
              status(result) must beEqualTo(UNAUTHORIZED)
            }
            case Left(error) => failure(error.message)
          }
          case Left(error) => failure(error.message)
        }
        case Left(error) => failure(error.message)
      }
      case Left(error) => failure(error.message)
    }
  }

  "can read a collection of items with read permission" in {
    Organization.insert(new Organization("test",Seq(),Seq()),None) match {
      case Right(org) => User.insertUser(new User("testoplenty","Test O'Plenty","testoplenty@gmail.com",Seq(),"abc123"),org.id,Permission.Read) match {
        case Right(user) => AccessToken.insertToken(new AccessToken(org.id,Some(user.userName),"testoplenty_token")) match {
          case Right(token) => ContentCollection.insertCollection(org.id, new ContentCollection("test"), Permission.Read) match {
            case Right(coll) => {
              val fakeRequest = FakeRequest(GET, "/api/v1/collections/"+coll.id.toString+"/items?access_token="+token.tokenId)
              val result = routeAndCall(fakeRequest).get
              ContentCollection.remove(coll)
              AccessToken.remove(token)
              User.remove(user)
              Organization.remove(org)
              status(result) must beEqualTo(OK)
            }
            case Left(error) => failure(error.message)
          }
          case Left(error) => failure(error.message)
        }
        case Left(error) => failure(error.message)
      }
      case Left(error) => failure(error.message)
    }
  }

  "cannot create an item in a collection with read permission" in {
    Organization.insert(new Organization("test",Seq(),Seq()),None) match {
      case Right(org) => User.insertUser(new User("testoplenty","Test O'Plenty","testoplenty@gmail.com",Seq(),"abc123"),org.id,Permission.Read) match {
        case Right(user) => AccessToken.insertToken(new AccessToken(org.id,Some(user.userName),"testoplenty_token")) match {
          case Right(token) => ContentCollection.insertCollection(org.id, new ContentCollection("test"), Permission.Read) match {
            case Right(coll) => {
              val toCreate = xmlBody("<html><feedbackInline></feedbackInline></html>", Map("collectionId" -> coll.id.toString))
              var fakeRequest = FakeRequest(POST, "/api/v1/items?access_token="+token.tokenId, FakeHeaders(), AnyContentAsJson(toCreate))
              var result = routeAndCall(fakeRequest).get
              ContentCollection.remove(coll)
              AccessToken.remove(token)
              User.remove(user)
              Organization.remove(org)
              status(result) must beEqualTo(UNAUTHORIZED)
            }
            case Left(error) => failure(error.message)
          }
          case Left(error) => failure(error.message)
        }
        case Left(error) => failure(error.message)
      }
      case Left(error) => failure(error.message)
    }
  }

  "can create an item in a collection with write permission" in {
    Organization.insert(new Organization("test",Seq(),Seq()),None) match {
      case Right(org) => User.insertUser(new User("testoplenty","Test O'Plenty","testoplenty@gmail.com",Seq(),"abc123"),org.id,Permission.Read) match {
        case Right(user) => AccessToken.insertToken(new AccessToken(org.id,Some(user.userName),"testoplenty_token")) match {
          case Right(token) => ContentCollection.insertCollection(org.id, new ContentCollection("test"), Permission.Write) match {
            case Right(coll) => {
              val title = "blergl mergl"
              val toCreate = xmlBody("<html><feedbackInline></feedbackInline></html>", Map("collectionId" -> coll.id.toString, Item.title -> "blergl mergl"))
              var fakeRequest = FakeRequest(POST, "/api/v1/items?access_token="+token.tokenId, FakeHeaders(), AnyContentAsJson(toCreate))
              var result = routeAndCall(fakeRequest).get
              val json = Json.parse(contentAsString(result))
              Content.collection.remove(MongoDBObject("_id" -> new ObjectId((json \ "id").as[String])))
              ContentCollection.remove(coll)
              AccessToken.remove(token)
              User.remove(user)
              Organization.remove(org)
              status(result) must beEqualTo(OK)
              (json \ Item.title).as[String] must beEqualTo(title)
            }
            case Left(error) => failure(error.message)
          }
          case Left(error) => failure(error.message)
        }
        case Left(error) => failure(error.message)
      }
      case Left(error) => failure(error.message)
    }
  }


}
