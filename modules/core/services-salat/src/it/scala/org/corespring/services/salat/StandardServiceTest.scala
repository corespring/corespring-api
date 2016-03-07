package org.corespring.services.salat

import org.bson.types.ObjectId
import org.corespring.models.{ Domain, Standard, StandardDomains }
import org.corespring.services.StandardQuery
import org.specs2.mutable.BeforeAfter
import org.specs2.time.NoTimeConversions

import scala.concurrent.Await
import scala.concurrent.duration._

class StandardServiceTest extends ServicesSalatIntegrationTest with NoTimeConversions {

  import Standard.Subjects._

  trait scope extends BeforeAfter {

    val service = services.standardService

    def addStandard(subject: String, category: String, dotNotation: String): Standard = {
      val s: Standard = Standard(
        subject = Some(subject),
        category = Some(category),
        subCategory = Some(category),
        dotNotation = Some(dotNotation))
      val id = service.insert(s).get
      s.copy(id = id)
    }

    override def before: Any = {
      addStandard(ELA, "ela-1", "C.1.2")
      addStandard(ELA, "ela-1", "C.1")
      addStandard(ELA, "ela-1", "C.1.1")
      addStandard(ELA, "ela-2", "C.2")
      logger.debug(s"function=before - standard insertion complete")
    }

    override def after = {
      removeAllData()
    }
  }

  "count" should {
    //same as dao.count
    "work" in pending
  }
  "delete" should {
    //same as dao.removeById
    "remove standard" in new scope {
      val standard = addStandard("subject-1", "category-1", "A.B.C")
      service.delete(standard.id)
      service.findOneById(standard.id) must_== None
    }
    "return true if standard has been removed" in new scope {
      val standard = addStandard("subject-1", "category-1", "A.B.C")
      service.delete(standard.id) must_== true
    }
    "return false if standard has not been removed" in new scope {
      service.delete(ObjectId.get) must_== false
    }
  }

  "domains" should {

    "return domains" in new scope {

      val expected = StandardDomains(
        Seq(
          Domain("ela-1", Seq("C.1", "C.1.1", "C.1.2")),
          Domain("ela-2", Seq("C.2"))),
        Seq.empty[Domain])

      val result = service.domains

      val inner = Await.result(result, 2.seconds)

      logger.debug(s"domains - inner result: $inner")

      result must equalTo(expected).await(timeout = 10.seconds)
    }
  }

  "find" should {
    //deprecated
    "work" in pending
  }

  "findOne" should {
    "find standard by id" in new scope {
      val standard = addStandard("subject-1", "category-1", "A.B.C")
      service.findOne(standard.id.toString) must_== Some(standard)
    }
    "return none if id is not an ObjectId" in new scope {
      service.findOne("not an object id") must_== None
    }
    "return none if standard cannot be found" in new scope {
      service.findOne(ObjectId.get.toString) must_== None
    }
  }

  "findOneByDotNotation" should {
    "find exactly matching standard by dot notation" in new scope {
      val standard = addStandard("subject-1", "category-1", "A.B.C")
      service.findOneByDotNotation("A.B.C") must_== Some(standard)
    }
    "return none if dotNotation does not match exactly" in new scope {
      val standard = addStandard("subject-1", "category-1", "A.B.C")
      service.findOneByDotNotation("A.") must_== None
    }
    "return none if dotNotation does not match any standard" in new scope {
      val standard = addStandard("subject-1", "category-1", "A.B.C")
      service.findOneByDotNotation("X.Y.Z") must_== None
    }
  }

  "findOneById" should {
    //same as dao.findOneById
    "work" in pending
  }

  "insert" should {
    //same as dao.insert
    "work" in pending
  }

  "list" should {
    //same as dao.find().skip.limit
    "work" in pending
  }

  "query" should {
    "return a list of standards, when term is found in dotNotation" in new scope {
      val query = StandardQuery("C.1.2", None, None, None, None)
      val stream = service.query(query, 0, 0)
      stream.length === 1
    }

    "return an empty list of standards when term is found in dotNotation, but the category does not match" in new scope {
      val query = StandardQuery("C.1", None, None, Some(Math), None)
      val stream = service.query(query, 0, 0)
      stream.length === 0
    }

    "return standards that match the dotNotation" in new scope {
      val query = StandardQuery("C.1", None, None, None, None)
      val stream = service.query(query, 0, 0)
      stream.length === 3
    }
  }

  "queryDotNotation" should {
    "exactly match the dotNotation" in new scope {
      val stream = service.queryDotNotation("C.1", 0, 0)
      stream.length === 1
    }
    "return empty stream if dotNotation does not match any standard" in new scope {
      val stream = service.queryDotNotation("not a dot notation", 0, 0)
      stream.length === 0
    }
  }

}

