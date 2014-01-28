package org.corespring.v2player.integration.transformers.qti.interactions.equation

import org.specs2.mutable.Specification
import play.api.libs.json.JsUndefined

class DomainParserTest extends Specification with DomainParser {

  "parseDomain" should {

    val (start, end) = (-10, 10)
    val excludedValues = Seq(1, 2, 3)

    "return empty object for empty string" in {
      val output = parseDomain("")
      output.fields.isEmpty must beTrue
    }

    "correctly parse included domains" in {
      val output = parseDomain(s"$start->$end")
      val included = output \ "included"
      val excluded = output \ "excluded"

      excluded must haveClass[JsUndefined]
      included must not(haveClass[JsUndefined])
      included.as[Seq[String]].contains(s"$start,$end") must beTrue
    }

    "correctly parse excluded numbers" in {
      val output = parseDomain(excludedValues.mkString(","))
      val included = output \ "included"
      val excluded = output \ "excluded"

      included must haveClass[JsUndefined]
      excluded must not(haveClass[JsUndefined])
      excluded.as[Seq[Int]] must be equalTo excludedValues
    }

    "correctly parse both included domains and excluded numbers" in {
      val output = parseDomain(s"$start->$end,${excludedValues.mkString(",")}")
      val included = output \ "included"
      val excluded = output \ "excluded"

      included must not(haveClass[JsUndefined])
      included.as[Seq[String]].contains(s"$start,$end") must beTrue

      excluded must not(haveClass[JsUndefined])
      excluded.as[Seq[Int]] must be equalTo excludedValues
    }

  }

}
