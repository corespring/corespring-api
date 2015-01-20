package org.corespring.platform.core.services

import org.corespring.it.IntegrationSpecification
import org.corespring.platform.core.models.Standard
import org.corespring.test.helpers.models.StandardHelper
import org.specs2.mutable.BeforeAfter
import play.api.libs.json.Json

class StandardQueryServiceTest extends IntegrationSpecification {

  "Standard Query Service" should {

    def makeQuery(s:String) = Json.obj("searchTerm" -> s).toString()

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

  }

  class StandardData(val standards: String*) extends BeforeAfter {

    lazy val standardIds = StandardHelper.create(standards.map(s => Standard(
      dotNotation = Some(s),
      subject = Some(s),
      category = Some(s),
      subCategory = Some(s),
      standard = Some(s))):_*)

    override def before: Any = {
      println(s"[before] mock standard id: $standardIds")
    }

    override def after: Any = {
      println(s"[after] delete $standardIds")
      StandardHelper.delete(standardIds)
    }
  }
}
