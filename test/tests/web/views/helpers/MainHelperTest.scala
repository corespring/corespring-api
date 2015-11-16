package tests.web.views.helpers

import org.bson.types.ObjectId
import org.specs2.mutable.Specification
import web.views.helpers.MainHelper
import org.corespring.platform.core.models.Organization
import org.corespring.test.PlaySingleton
import org.corespring.common.utils.string

class MainHelperTest extends Specification {

  PlaySingleton.start

  "main helper" should {

    "write org json" in {

      val id = ObjectId.get()
      val org = Organization(id = id, name = "test")
      val template = """{"id":"${id}","isRoot":false,"name":"test","collections":[]}"""
      val expected = string.interpolate(template, string.replaceKey(Map("id" -> id.toString)), string.DollarRegex)
      MainHelper.toFullJson(org).body === expected
    }

    "write safe xml " in {
      val xml = <root name="test">hello \ there \n</root>
      MainHelper.safeXml(xml.toString).body === """<root name=\"test\">hello \\ there \\n<\/root>"""
    }
  }

}