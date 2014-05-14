package org.corespring.platform.core.controllers.auth

import org.specs2.mutable.Specification

class TokenReaderTest extends Specification {

  "TokenReader" should{

    "read" in {


      reader.getToken(requst) match {
        case Right(token) => ...
        case Left(error) => ...
      }
    }
  }

}
