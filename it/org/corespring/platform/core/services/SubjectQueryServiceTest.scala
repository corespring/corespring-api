package org.corespring.platform.core.services

import org.corespring.it.IntegrationSpecification
import org.corespring.test.helpers.models.SubjectHelper
import org.specs2.mutable.BeforeAfter

class SubjectQueryServiceTest extends IntegrationSpecification {

  "Subject Query Service" should {

    "handle" in new SubjectData("some test subject") {
      val result = SubjectQueryService.query("some test")
      result.length === 1
    }

    "handle multiple" in new SubjectData("!! 1", "!! 2") {
      SubjectQueryService.query("!!").length === 2
      SubjectQueryService.query("!!!").length === 0
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
