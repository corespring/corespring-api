package tests.web.views.helpers

import common.utils.string
import models.Organization
import org.bson.types.ObjectId
import org.specs2.mutable.Specification
import tests.PlaySingleton
import web.views.helpers.MainHelper

class MainHelperTest extends Specification {

  PlaySingleton.start

  "main helper" should {

    "write org json" in {

      val id = ObjectId.get()
      val orgs = Seq(Organization(id = id, name = "test"))

      val template = """[{"id":"${id}","name":"test","collections":[]}]"""
      val expected = string.interpolate(template, string.replaceKey(Map("id" -> id.toString)), string.DollarRegex)
      MainHelper.toFullJson(orgs).text === expected
    }

    "write safe xml " in {
      val xml = <root name="test"> hello \ there \n </root>
      MainHelper.safeXml(xml.toString).text === """<root name=\"test\"> hello \\ there \\n </root>"""
    }
  }

}
