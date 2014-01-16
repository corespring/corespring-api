package org.corespring.v2player.integration

import org.bson.types.ObjectId
import org.corespring.it.ITSpec
import org.corespring.platform.core.models.User

class OtherTest extends ITSpec {


  "z" should {
    "z" in {
      true === true
    }
  }

  "insert user" should {

    "insert" in {

      val userId = ObjectId.get
      User.insert(User(id = userId))
      User.findOneById(userId).isDefined === true
    }
  }
}
