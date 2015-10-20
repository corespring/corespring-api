package org.corespring.services.salat

import org.corespring.models.Subject
import org.corespring.services.SubjectQuery
import org.specs2.mutable.Before

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

}
