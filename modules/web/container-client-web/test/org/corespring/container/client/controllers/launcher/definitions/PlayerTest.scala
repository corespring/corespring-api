package org.corespring.container.client.controllers.launcher.definitions

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

class CatalogTest extends Specification with Mockito {

  trait scope extends Scopes.launcher
  "result" should {
    //can i run this and avoid booting up a play app?
    "call builder.buildJs" in pending
  }
}

class PlayerTest extends Specification with Mockito {

  trait scope extends Scopes.launcher
  "result" should {
    //can i run this and avoid booting up a play app?
    "call builder.buildJs" in pending
  }
}