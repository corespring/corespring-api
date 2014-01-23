package org.corespring.v2player.integration.transformers.qti

import play.api.libs.json.JsValue
import scala.xml.{Elem, Node}
import scala.xml.transform.RuleTransformer
import org.corespring.v2player.integration.transformers.qti.interactions._
import play.api.libs.json.JsObject
import scala.collection.mutable

object QtiTransformer extends XMLNamespaceClearer {

  def transform(qti: Elem): (Node,JsValue) = {

    val components : mutable.Map[String,JsObject] = new mutable.HashMap[String,JsObject]()

    val transformedHtml = new RuleTransformer(
      new ChoiceInteractionTransformer(components, qti),
      new TextEntryInteractionTransformer(components, qti),
      new DragAndDropInteractionTransformer(components, qti),
      new OrderInteractionTransformer(components, qti),
      new PointInteractionTransformer(components, qti),
      new FocusTaskInteractionTransformer(components, qti),
      FoldableInteractionTransformer,
      CoverflowInteractionTransformer
    ).transform(qti)

    val html = clearNamespace((transformedHtml(0) \ "itemBody")(0))

    (html, JsObject(components.toSeq))
  }
}


