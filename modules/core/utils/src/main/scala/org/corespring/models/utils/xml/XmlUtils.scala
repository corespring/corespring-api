package org.corespring.models.utils.xml

object XmlUtils {

  val ScriptRegex = "(?s)<script(.*?)>(.*?)</script>".r
  val CDataRegex = """(?s).*?<script.*?>.*?<!\[CDATA(.*?)\]\]>.*?</script>.*?""".r

  /**
   * Analyzes a String containing markup, and replaces all instances of
   *   <script>
   *     // script body
   *   </script>
   *
   * with
   *   <script>
   *     <![CDATA[
   *      // script body
   *     ]]>
   *   </script>
   *
   * where CData tags are not already present
   */
  def addCDataTags(markup: String): String = ScriptRegex.replaceSomeIn(markup, { m =>
    m.matched match {
      case CDataRegex(_*) => None
      case ScriptRegex(attributes, value) => Some(s"<script$attributes><![CDATA[$value]]></script>")
      case _ => None
    }
  })

  def trim(xml: String) = xml.trim
}
