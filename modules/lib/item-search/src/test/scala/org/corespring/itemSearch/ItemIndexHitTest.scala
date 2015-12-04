package org.corespring.itemSearch

import org.specs2.mutable.Specification
import java.io.InputStream

import org.specs2.specification.Scope
import play.api.libs.json.{ JsValue, Json }

import scala.io.{ Codec, Source }

class ItemIndexHitTest extends Specification {

  trait scope extends Scope {
    val sut = ItemIndexHit

    def readFile(path: String): JsValue = {
      val stream: InputStream = getClass.getResourceAsStream(path)
      Json.parse(Source.fromInputStream(stream)(Codec.UTF8).mkString)
    }
  }

  "reads" should {
    "succeed" in new scope {
      sut.Format.reads(readFile("/failing-hit.json"))
    }
  }
}
