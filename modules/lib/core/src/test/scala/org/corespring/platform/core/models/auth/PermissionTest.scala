package org.corespring.platform.core.models.auth

import org.specs2.mutable.Specification

class PermissionTest extends Specification {


  "Permission" should {

    "work" in {


      Permission.Read.has(Permission.Write) === false
    }
  }

}
