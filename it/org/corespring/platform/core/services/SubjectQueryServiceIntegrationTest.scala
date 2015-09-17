package org.corespring.platform.core.services

import org.corespring.it.IntegrationSpecification
import org.corespring.it.helpers.SubjectHelper
import org.corespring.models.Subject
import org.specs2.mutable.BeforeAfter
import play.api.libs.json.Json

class subjectServiceIntegrationTest extends IntegrationSpecification {

  lazy val subjectService = bootstrap.Main.subjectService

  "Subject Query Service" should {

    def makeQuery(s: String) = Json.obj("searchTerm" -> s).toString()

    "be able to find single items" in new SubjectData("!!some test subject!!") {
      val result = subjectService.query(makeQuery("!!some test"))
      result.length === 1
    }

    "be able to find multiple items" in new SubjectData("!! 1", "!! 2") {
      subjectService.query(makeQuery("!!")).length === 2
      subjectService.query(makeQuery("!!!")).length === 0
    }

    "ignores case" in new SubjectData("!!UPPERCASE!!", "!!lowercase!!") {
      subjectService.query(makeQuery("case!!")).length === 2
      subjectService.query(makeQuery("CASE!!")).length === 2
    }

    "search find item from category" in new SubjectData(Subject(category = Some("!!good"), subject = "!!bad")) {
      subjectService.query(makeQuery("!!good")).length === 1
    }

    "search find item from subject" in new SubjectData(Subject(category = Some("!!bad"), subject = "!!good")) {
      subjectService.query(makeQuery("!!good")).length === 1
    }

  }

  class SubjectData(val subjects: Any*) extends BeforeAfter {

    def toSubjects(values: Any*) = values.map((v: Any) => v match {
      case s: String => Subject(category = Some(s), subject = s)
      case o: Subject => o
    })

    lazy val subjectIds = SubjectHelper.create(toSubjects(subjects: _*): _*)

    override def before: Any = {
      println(s"[before] mock subject id: $subjectIds")
    }

    override def after: Any = {
      println(s"[after] delete $subjectIds")
      SubjectHelper.delete(subjectIds)
    }
  }

}
