package org.corespring.qtiToV2.kds

import org.apache.commons.lang3.StringEscapeUtils

import scala.io.Source
import scala.xml._

trait PassageTransformer {

  def transformPassage(resource: ManifestResource)(implicit sources: Map[String, Source]): Option[String] = {
    resource.resourceType == ManifestResourceType.Passage match {
      case true => {
        sources.find{ case (path, source) => resource.path == path }.map(_._2) match {
          case Some(source) => Some(transformPassage(source.getLines.mkString("\n")))
          case _ => None
        }
      }
      case _ => None
    }
  }

  private def stripCDataTags(xmlString: String) =
    StringEscapeUtils.unescapeHtml4("""(?s)<!\[CDATA\[(.*?)\]\]>""".r.replaceAllIn(xmlString, "$1"))

  private def transformPassage(xmlString: String): String = {
    <div class="passage">{
      (XML.loadString(stripCDataTags(xmlString)) \ "passageBody" \\ "passageParts" \\ "partBlock").map(pb => <div/>.copy(child = pb))
    }</div>.toString
  }

}
