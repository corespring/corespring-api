package org.corespring.platform.core.models

import controllers.auth.Permission
import org.bson.types.ObjectId
import org.specs2.mutable.{After, Specification}
import tests.PlaySingleton

class UserTest extends Specification {

  PlaySingleton.start

  sequential

  class UserScoped extends After{

    lazy val user = User("user_test_name")
    lazy val org = getOrg

    def getOrg = {
      val o = Organization(id = ObjectId.get, name = "user test org")
      Organization.insert(o)
      o
    }

    def after{
      Organization.remove(org)
      User.removeUser(user.userName)
    }
  }

  "user" should {

    "insert" in pending
    "update" in pending
    "add organization" in pending
    "get organization" in pending
    "get" in pending
    "write json" in pending

    "get permissions" in new UserScoped {
      User.insertUser(user, org.id, Permission.Write)
      User.getPermissions(user.userName, org.id) === Right(Permission(3,"write"))
    }
  }

}
