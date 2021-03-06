package org.corespring.models.utils.xml

import org.specs2.mutable.Specification

class XmlUtilsTest extends Specification {

  "addCDataTags" should {

    "replace <script/> with <script><![CDATA[]]></script>" in {
      val script =
        """
          <script type="text/javascript">
            console.log("Javascript!");
          </script>
        """

      XmlUtils.addCDataTags(script) match {
        case XmlUtils.CDataRegex(_) => success
        case _ => failure("Did not match regex for CData tags")
      }
    }

    "do nothing to <script><![CDATA[]]></script>" in {
      val script =
        """
          <script type="text/javascript">
            <![CDATA[
              console.log("Javascript!");
            ]]>
          </script>
        """

      XmlUtils.addCDataTags(script) === script
    }

    "replace multiple <script/> with <script><![CDATA[]]></script>" in {
      val script =
        """
          <script type="text/javascript">
            console.log("Javascript!");
          </script>
        """

      XmlUtils.CDataRegex.findAllIn(XmlUtils.addCDataTags(s"$script$script")).length === 2
    }

  }

}

