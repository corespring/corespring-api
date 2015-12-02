package org.corespring.services.salat

import com.mongodb.DBObject
import org.bson.types.ObjectId
import org.corespring.models.Subject
import org.corespring.services.SubjectQuery
import org.specs2.mutable.{ BeforeAfter, Before }

class SubjectServiceTest extends ServicesSalatIntegrationTest {

  trait scope extends BeforeAfter {

    val service = services.subjectService

    protected def addSubject(subjectName: String, category: Option[String] = None): Subject = {
      val subject = Subject(subjectName, category)
      val id = service.insert(subject).get
      subject.copy(id = id)
    }

    protected var subjects: Seq[Subject] = Seq.empty

    override def before: Any = {
      subjects = Seq(
        addSubject("History", Some("Humanities")),
        addSubject("Geography", Some("Science")),
        addSubject("Art", Some("Arts and Crafts")))
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
    "work" in pending
  }
  "find" should {
    //deprecated
    "work" in pending
  }
  "findOne" should {
    "find standard by id" in new scope {
      val s = addSubject("subject-1")
      service.findOne(s.id.toString) must_== Some(s)
    }
    "return none if id is not an ObjectId" in new scope {
      service.findOne("not an object id") must_== None
    }
    "return none if standard cannot be found" in new scope {
      service.findOne(ObjectId.get.toString) must_== None
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

  "query(SubjectQuery)" should {

    "match term against subjects, when no subject is used" in new scope {
      val query = SubjectQuery("Hi", None, None)
      val stream = service.query(query, 0, 0)
      stream.length must_== 1
      stream(0) must_== subjects(0)
    }
    "match term against category, when no category is used" in new scope {
      val query = SubjectQuery("Sci", None, None)
      val stream = service.query(query, 0, 0)
      stream.length must_== 1
      stream(0) must_== subjects(1)
    }
    "filter matched results against subject, if provided" in new scope {
      val query = SubjectQuery("Hi", Some("Does not exist"), None)
      val stream = service.query(query, 0, 0)
      stream.length must_== 0
    }
    "find exact match by subject" in new scope {
      val query = SubjectQuery("", Some("History"), None)
      val stream = service.query(query, 0, 0)
      stream.length must_== 1
      stream(0) must_== subjects(0)
    }
    "find exact match by category" in new scope {
      val query = SubjectQuery("", None, Some("Science"))
      val stream = service.query(query, 0, 0)
      stream.length must_== 1
      stream(0) must_== subjects(1)
    }
    "find exact match by subject and filter category by term" should {
      "find item if term is a match for category" in new scope {
        val query = SubjectQuery("Humanit", Some("History"), None)
        val stream = service.query(query, 0, 0)
        stream.length must_== 1
      }
      "find nothing if term does not match category" in new scope {
        val query = SubjectQuery("Does not exist", Some("History"), None)
        val stream = service.query(query, 0, 0)
        stream.length must_== 0
      }
    }
    "find exact match by category and filter subjects by term" should {
      "find item if term is a match for subject" in new scope {
        val query = SubjectQuery("Geograp", None, Some("Science"))
        val stream = service.query(query, 0, 0)
        stream.length must_== 1
      }
      "find nothing if term does not match subject" in new scope {
        val query = SubjectQuery("Does not exist", None, Some("Science"))
        val stream = service.query(query, 0, 0)
        stream.length must_== 0
      }
    }

  }

}
