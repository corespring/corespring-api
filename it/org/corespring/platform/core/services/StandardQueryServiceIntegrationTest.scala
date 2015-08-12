package org.corespring.platform.core.services

import org.corespring.it.IntegrationSpecification
import org.corespring.platform.core.models.{ Standard }
import org.corespring.test.helpers.models.{ StandardHelper }
import org.specs2.mutable.BeforeAfter
import play.api.libs.json.Json

class StandardQueryServiceIntegrationTest extends IntegrationSpecification {

  "Standard Query Service" should {

    def makeQuery(s: String) = Json.obj("searchTerm" -> s).toString()

    ""
    "be able to find single items" in new StandardData("some test standard") {
      val result = StandardQueryService.query(makeQuery("some test"))
      result.length === 1
    }

    "be able to find multiple items" in new StandardData("!! 1", "!! 2") {
      StandardQueryService.query(makeQuery("!!")).length === 2
      StandardQueryService.query(makeQuery("!!!")).length === 0
    }

    "ignores case" in new StandardData("1.X.Y.Z", "1.x.y.z") {
      StandardQueryService.query(makeQuery("1.x.y.z")).length === 2
      StandardQueryService.query(makeQuery("1.X.Y.Z")).length === 2
    }

    "be able to find standard by subject" in new StandardData(
      Standard(subject = Some("!!ralf's subject test!!"))) {
      StandardQueryService.query(makeQuery("ralf's subject")).length === 1
    }

    "be able to find standard by category" in new StandardData(
      Standard(category = Some("!!ralf's category test!!"))) {
      StandardQueryService.query(makeQuery("ralf's category")).length === 1
    }

    "be able to find standard by subCategory" in new StandardData(
      Standard(subCategory = Some("!!ralf's subCategory!!"))) {
      StandardQueryService.query(makeQuery("ralf's subCategory")).length === 1
    }

    "be able to find standard by standard" in new StandardData(
      Standard(standard = Some("!!ralf's standard!!"))) {
      StandardQueryService.query(makeQuery("ralf's standard")).length === 1
    }

    "be able to find standard by dotNotation" in new StandardData(
      Standard(dotNotation = Some("!!ralf's dotNotation!!"))) {
      StandardQueryService.query(makeQuery("ralf's dotNotation")).length === 1
    }

    "be able to filter by subject" in new StandardData(
      Standard(subject = Some("!!ralf's test subject!!"), standard = Some("one")),
      Standard(subject = Some("!!ralf's test subject!!"), standard = Some("two"))) {
      StandardQueryService.query(
        Json.obj(
          "searchTerm" -> "",
          "filters" -> Json.obj("subject" -> "!!ralf's test subject!!")).toString()).length === 2
    }

  }

  class StandardData(val standard: Any*) extends BeforeAfter {

    def toStandard(values: Any*) = values.map((v: Any) => v match {
      case s: String => Standard(
        dotNotation = Some(s),
        subject = Some(s),
        category = Some(s),
        subCategory = Some(s),
        standard = Some(s))
      case o: Standard => o
    })

    lazy val standardIds = StandardHelper.create(toStandard(standard: _*): _*)

    override def before: Any = {
      println(s"[before] mock standard id: $standardIds")
    }

    override def after: Any = {
      println(s"[after] delete $standardIds")
      StandardHelper.delete(standardIds)
    }
  }
}
