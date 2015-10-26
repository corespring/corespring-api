package org.corespring.services.salat

import com.mongodb.{ WriteConcern, DBObject }
import com.mongodb.casbah.MongoCollection
import com.novus.salat.dao.{ SalatMongoCursor, SalatDAO }
import org.bson.types.ObjectId
import org.corespring.errors.PlatformServiceError
import org.corespring.models.{ Organization, UserOrg, User }
import org.corespring.models.auth.Permission
import org.corespring.salat.config.SalatContext
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scalaz.{ Success, Failure }

class UserServiceTest extends Specification with Mockito {

  trait scope extends Scope {

    lazy val user = User("john.doe", "John Doe", "john.doe@gmail.com", org = UserOrg(ObjectId.get, Permission.Write.value))

    val dao = {
      val m = mock[SalatDAO[User, ObjectId]]
      m.findOne(any[Any])(any[Any => DBObject]) returns None
      m.insert(any[User], any[WriteConcern]) returns Some(user.id)
      m.collection returns {
        mock[MongoCollection].writeConcern returns {
          mock[WriteConcern]
        }
      }
      m
    }

    val context = new SalatContext(this.getClass.getClassLoader)

    val orgService = {
      val m = mock[OrganizationService]
      m.findOneById(any[ObjectId]) returns Some(mock[Organization])
      m
    }

    val service = new UserService(dao, context, orgService)

  }

  "insertUser" should {

    trait insertUser extends scope

    "return an error if the org can't be found" in new insertUser {
      orgService.findOneById(any[ObjectId]) returns None
      service.insertUser(user) must_== Failure(_: PlatformServiceError)
    }

    "return an error if the userName is taken" in new insertUser {
      dao.findOne(any[Any])(any[Any => DBObject]) returns Some(mock[User])
      service.insertUser(user) must_== Failure(_: PlatformServiceError)
    }

    "return an error if the insert fails" in new insertUser {
      dao.insert(any[User]) returns None
      service.insertUser(user) must_== Failure(_: PlatformServiceError)
    }

    "return an error if the insert fails" in new insertUser {
      dao.insert(any[User]) returns None
      service.insertUser(user) must_== Failure(_: PlatformServiceError)
    }

    "returns the new user id" in new insertUser {
      service.insertUser(user) must_== Success(user)
    }
  }

  "updateUser" should {

    trait updateUser extends scope {
      val update = user.copy(userName = "update")
      val existingUser = user.copy(id = ObjectId.get, userName = "update")

      def mockCursor(users: User*): SalatMongoCursor[User] = {
        val c = mock[SalatMongoCursor[User]]
        c.toSeq returns users.toSeq
        c
      }
    }

    "not update if the new userName is already taken" in new updateUser {
      dao.find(any[Any])(any[Any => DBObject]) returns mockCursor(existingUser)
      service.updateUser(update) must_== Failure(_: PlatformServiceError)
    }

    //Simulate multiple users with the same username - this will be removed soon: see: PE-387
    "not update if the new userName is already taken (multiple users)" in new updateUser {
      dao.find(any[Any])(any[Any => DBObject]) returns mockCursor(existingUser, existingUser)
      service.updateUser(update) must_== Failure(_: PlatformServiceError)
    }

    "update if the update is for the same user" in new updateUser {
      dao.find(any[Any])(any[Any => DBObject]) returns mockCursor(user)
      service.updateUser(user.copy(fullName = "update")) must_== Success(_: User)
    }
  }
}
