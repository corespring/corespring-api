package org.corespring.container.client.controllers.launcher.definitions

import org.corespring.container.client.controllers.launcher.JsBuilder
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import play.api.libs.json.JsObject

private[definitions] object Scopes extends Mockito {

  trait launcher extends Scope {
    lazy val builder = {
      val m = mock[JsBuilder]
      m.buildJs(any[String], any[Seq[String]], any[JsObject], any[String], any[Map[String, String]]) returns {
        "//js"
      }
      m
    }
  }
}
