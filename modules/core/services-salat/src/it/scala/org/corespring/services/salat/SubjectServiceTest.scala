package org.corespring.services.salat

import org.corespring.models.Subject
import org.corespring.services.SubjectQuery
import org.specs2.mutable.{ BeforeAfter, Before }
import play.api.libs.json.Json

class SubjectServiceTest extends ServicesSalatIntegrationTest {

  trait scope extends Before {

    protected def addSubject(subjectName: String, category: Option[String] = None) = {
      val subject = Subject(subjectName, category)
      services.subjectService.insert(subject).map { id =>
        subject.copy(id = id)
      }
    }

    protected var subjects: Seq[Subject] = Seq.empty

    override def before: Any = {
      logger.debug(s"function=before - seed some subjects")
      subjects = Seq(
        addSubject("History", Some("Humanities")),
        addSubject("Geography", Some("Science")),
        addSubject("Art", Some("Arts and Crafts"))).flatten

      logger.debug(s"function=before - seeding subjects completed")
    }
  }

  "query(SubjectQuery)" should {

    "return a list of subjects" in new scope {
      val query = SubjectQuery("Hi", None, None)
      val stream = services.subjectService.query(query, 0, 0)
      stream.length === 1

      stream(0) === subjects(0)
    }
  }

  /** Note: Moved from SubjectQueryServiceIntegrationTest */
  "@deprecated query(String)" should {

    def makeQuery(s: String) = Json.obj("searchTerm" -> s).toString()

    "be able to find single items" in new SubjectData("!!some test subject!!") {
      val result = services.subjectService.query(makeQuery("!!some test"))
      result.length === 1
    }

    "be able to find multiple items" in new SubjectData("!! 1", "!! 2") {
      services.subjectService.query(makeQuery("!!")).length === 2
      services.subjectService.query(makeQuery("!!!")).length === 0
    }

    "ignores case" in new SubjectData("!!UPPERCASE!!", "!!lowercase!!") {
      services.subjectService.query(makeQuery("case!!")).length === 2
      services.subjectService.query(makeQuery("CASE!!")).length === 2
    }

    "search find item from category" in new SubjectData(Subject(category = Some("!!good"), subject = "!!bad")) {
      services.subjectService.query(makeQuery("!!good")).length === 1
    }

    "search find item from subject" in new SubjectData(Subject(category = Some("!!bad"), subject = "!!good")) {
      services.subjectService.query(makeQuery("!!good")).length === 1
    }

  }

  class SubjectData(val subjects: Any*) extends BeforeAfter {

    def toSubjects(values: Any*) = values.map((v: Any) => v match {
      case s: String => Subject(category = Some(s), subject = s)
      case o: Subject => o
    })

    lazy val subjectIds = toSubjects(subjects: _*).flatMap { s =>
      services.subjectService.insert(s)
    }

    override def before: Any = {
      clearDb()
      logger.debug(s"[before] mock subject id: $subjectIds")
    }

    override def after: Any = {
      logger.debug(s"[after] delete $subjectIds")
    }
  }

}
