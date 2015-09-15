package org.corespring.platform.core.services

import org.corespring.it.IntegrationSpecification
import org.corespring.models.Subject
import org.corespring.test.helpers.models.SubjectHelper
import org.specs2.mutable.BeforeAfter
import play.api.libs.json.Json

class SubjectQueryServiceIntegrationTest extends IntegrationSpecification {

  "Subject Query Service" should {

    def makeQuery(s: String) = Json.obj("searchTerm" -> s).toString()

    "be able to find single items" in new SubjectData("!!some test subject!!") {
      val result = SubjectQueryService.query(makeQuery("!!some test"))
      result.length === 1
    }

    "be able to find multiple items" in new SubjectData("!! 1", "!! 2") {
      SubjectQueryService.query(makeQuery("!!")).length === 2
      SubjectQueryService.query(makeQuery("!!!")).length === 0
    }

    "ignores case" in new SubjectData("!!UPPERCASE!!", "!!lowercase!!") {
      SubjectQueryService.query(makeQuery("case!!")).length === 2
      SubjectQueryService.query(makeQuery("CASE!!")).length === 2
    }

    "search find item from category" in new SubjectData(Subject(category = Some("!!good"), subject = Some("!!bad"))) {
      SubjectQueryService.query(makeQuery("!!good")).length === 1
    }

    "search find item from subject" in new SubjectData(Subject(category = Some("!!bad"), subject = Some("!!good"))) {
      SubjectQueryService.query(makeQuery("!!good")).length === 1
    }

  }

  class SubjectData(val subjects: Any*) extends BeforeAfter {

    def toSubjects(values: Any*) = values.map((v: Any) => v match {
      case s: String => Subject(
        category = Some(s),
        subject = Some(s))
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
