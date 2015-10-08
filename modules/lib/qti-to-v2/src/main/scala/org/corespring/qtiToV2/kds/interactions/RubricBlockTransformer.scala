package org.corespring.qtiToV2.kds.interactions

import org.corespring.platform.core.models.item.resource.{ VirtualFile, Resource }
import org.corespring.qtiToV2.interactions.InteractionTransformer
import play.api.libs.json.JsObject

import scala.xml.Node

object RubricBlockTransformer extends InteractionTransformer {

  override def transform(node: Node, manifest: Node) = node match {
    case node: Node if (Seq("rubricBlock", "sampleBlock").contains(node.label)) => Seq.empty[Node]
    case _ => node
  }

  /**
   * TODO: Persist this as well
   */
  def getRubric(qti: Node): Option[Resource] = {
    (qti \\ "rubricBlock").length match {
      case 0 => None
      case _ => Some(Resource(
        name = "Rubric",
        files = Seq(new VirtualFile(
          name = s"${(qti \\ "assessmentItem" \ "@identifier").text}-rubric.xml",
          contentType = "text/xml",
          isMain = true,
          content = (qti \\ "rubricBlock").mkString))))
    }
  }

  override def interactionJs(qti: Node, manifest: Node): Map[String, JsObject] = Map.empty

}
