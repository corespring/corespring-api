package web.views.helpers

import org.bson.types.ObjectId
import org.corespring.models.Organization
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.libs.json.Writes

class MainHelperTest extends Specification with Mockito {

  val mockWrites = mock[Writes[Organization]]
  val helper = new MainHelper {
    override implicit def writeOrg: Writes[Organization] = mockWrites
  }

  "main helper" should {

    "write org json" in {
      val org = Organization(id = ObjectId.get, name = "test")
      helper.toFullJson(org)
      there was one(mockWrites).writes(org)
    }

    "write safe xml " in {
      val xml = <root name="test">hello \ there \n</root>
      helper.safeXml(xml.toString).body === """<root name=\"test\">hello \\ there \\n<\/root>"""
    }
  }
}
