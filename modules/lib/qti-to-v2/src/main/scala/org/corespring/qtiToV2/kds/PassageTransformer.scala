package org.corespring.qtiToV2.kds

import org.apache.commons.lang3.StringEscapeUtils
import org.corespring.qtiToV2.SourceWrapper

import scala.xml._

trait PassageTransformer extends PassageScrubber {

  import PathFlattener._

  def transformPassage(resource: ManifestResource)(implicit sources: Map[String, SourceWrapper]): Option[String] = {
    resource.resourceType == ManifestResourceType.Passage match {
      case true => {
        sources.find{ case (path, source) => resource.path.flattenPath == path.flattenPath }.map(_._2) match {
          case Some(source) => Some(transformPassage(source.getLines.mkString("\n")))
          case _ => throw new Exception(s"Missing passage ${resource.path}")
        }
      }
      case _ => None
    }
  }

  private def stripCDataTags(xmlString: String) =
    StringEscapeUtils.unescapeHtml4("""(?s)<!\[CDATA\[(.*?)\]\]>""".r.replaceAllIn(xmlString, "$1"))

  private def transformPassage(xmlString: String): String = {
    <div class="passage">{
      (XML.loadString(scrub(stripCDataTags(xmlString))) \ "passageBody" \\ "passageParts" \\ "partBlock").map(pb => <div/>.copy(child = pb))
    }</div>.toString
  }

}