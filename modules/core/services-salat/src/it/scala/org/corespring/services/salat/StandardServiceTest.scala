package org.corespring.services.salat

import com.mongodb.casbah.commons.MongoDBObject
import org.corespring.models.{ Domain, StandardDomains, Standard }
import org.corespring.services.StandardQuery
import org.specs2.mutable.{ Before }
import org.specs2.time.NoTimeConversions
import play.api.libs.json.Json
import scala.concurrent.Await
import scala.concurrent.duration._

class StandardServiceTest extends ServicesSalatIntegrationTest with NoTimeConversions {

  import Standard.Subjects._

  trait scope extends Before {

    def addStandard(subject: String, category: String, dotNotation: String): Standard = {
      val s: Standard = Standard(
        subject = Some(subject),
        category = Some(category),
        subCategory = Some(category),
        dotNotation = Some(dotNotation))
      val id = services.standardService.insert(s).get
      s.copy(id = id)
    }

    override def before: Any = {
      clearDb()
      addStandard(ELA, "ela-1", "C.1.2")
      addStandard(ELA, "ela-1", "C.1")
      addStandard(ELA, "ela-1", "C.1.1")
      addStandard(ELA, "ela-2", "C.2")
      logger.debug(s"function=before - standard insertion complete")
    }
  }

  "query(StandardQuery)" should {

    "return a list of standards when term is found in dotNotation" in new scope {
      val query = StandardQuery("C.1.2", None, None, None, None)
      val stream = services.standardService.query(query, 0, 0)
      stream.length === 1
    }

    "return an empty list of standards when term is found in dotNotation, but the category filter is wrong" in new scope {
      val query = StandardQuery("C.1.2", None, None, Some(Math), None)
      val stream = services.standardService.query(query, 0, 0)
      stream.length === 0
    }
  }

  class StandardData(val standard: Any*) extends Before {

    def toStandard(values: Any*) = values.map((v: Any) => v match {
      case s: String => Standard(
        dotNotation = Some(s),
        subject = Some(s),
        category = Some(s),
        subCategory = Some(s),
        standard = Some(s))
      case o: Standard => o
    })

    override def before: Any = {
      clearDb()
      toStandard(standard: _*).flatMap { s =>
        services.standardService.insert(s)
      }
    }
  }

  /** Note: Moved from StandardServiceIntegrationTest */
  "@deprecated query(String)" should {

    def makeQuery(s: String) = Json.obj("searchTerm" -> s).toString()

    "be able to find single items" in new StandardData("some test standard") {

      logger.debug(s"count -> ${services.standardService.count(MongoDBObject.empty)}")
      val result = services.standardService.query(makeQuery("some test"))
      result.length === 1
    }

    "be able to find multiple items" in new StandardData("!! 1", "!! 2") {
      services.standardService.query(makeQuery("!!")).length === 2
      services.standardService.query(makeQuery("!!!")).length === 0
    }

    "ignores case" in new StandardData("1.X.Y.Z", "1.x.y.z") {
      services.standardService.query(makeQuery("1.x.y.z")).length === 2
      services.standardService.query(makeQuery("1.X.Y.Z")).length === 2
    }

    "be able to find standard by subject" in new StandardData(
      Standard(subject = Some("!!ralf's subject test!!"))) {
      services.standardService.query(makeQuery("ralf's subject")).length === 1
    }

    "be able to find standard by category" in new StandardData(
      Standard(category = Some("!!ralf's category test!!"))) {
      services.standardService.query(makeQuery("ralf's category")).length === 1
    }

    "be able to find standard by subCategory" in new StandardData(
      Standard(subCategory = Some("!!ralf's subCategory!!"))) {
      services.standardService.query(makeQuery("ralf's subCategory")).length === 1
    }

    "be able to find standard by standard" in new StandardData(
      Standard(standard = Some("!!ralf's standard!!"))) {
      services.standardService.query(makeQuery("ralf's standard")).length === 1
    }

    "be able to find standard by dotNotation" in new StandardData(
      Standard(dotNotation = Some("!!ralf's dotNotation!!"))) {
      services.standardService.query(makeQuery("ralf's dotNotation")).length === 1
    }

    "be able to filter by subject" in new StandardData(
      Standard(subject = Some("!!ralf's test subject!!"), standard = Some("one")),
      Standard(subject = Some("!!ralf's test subject!!"), standard = Some("two"))) {
      services.standardService.query(
        Json.obj(
          "searchTerm" -> "",
          "filters" -> Json.obj("subject" -> "!!ralf's test subject!!")).toString()).length === 2
    }

  }

  "domains" should {

    "return domains" in new scope {

      val expected = StandardDomains(
        Seq(
          Domain("ela-1", Seq("C.1", "C.1.1", "C.1.2")),
          Domain("ela-2", Seq("C.2"))),
        Seq.empty[Domain])

      val result = services.standardService.domains

      val inner = Await.result(result, 2.seconds)

      logger.debug(s"domains - inner result: $inner")

      result must equalTo(expected).await(timeout = 10.seconds)
    }
  }
}

