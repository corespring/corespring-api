package org.corespring.platform.core.services

import org.corespring.it.IntegrationSpecification
import org.corespring.test.helpers.models.SubjectHelper
import org.specs2.mutable.BeforeAfter
import play.api.libs.json.Json

class SubjectQueryServiceTest extends IntegrationSpecification {

  "Subject Query Service" should {

    def makeQuery(s:String) = Json.obj("searchTerm" -> s).toString()

    "handle" in new SubjectData("some test subject") {
      val result = SubjectQueryService.query(makeQuery("some test"))
      result.length === 1
    }

    "handle multiple" in new SubjectData("!! 1", "!! 2") {
      SubjectQueryService.query(makeQuery("!!")).length === 2
      SubjectQueryService.query(makeQuery("!!!")).length === 0
    }

    "ignores case" in new SubjectData("UPPERCASE", "lowercase") {
      SubjectQueryService.query(makeQuery("case")).length === 2
      SubjectQueryService.query(makeQuery("CASE")).length === 2
    }

  }

  class SubjectData(val subjects: String*) extends BeforeAfter {

    lazy val subjectIds = SubjectHelper.create(subjects: _*)

    override def before: Any = {
      println(s"[before] mock subject id: $subjectIds")
    }

    override def after: Any = {
      println(s"[after] delete $subjectIds")
      SubjectHelper.delete(subjectIds)
    }
  }
}
