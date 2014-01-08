package org.corespring.poc.integration.impl.transformers.qti

import org.corespring.poc.integration.impl.transformers.qti.interactions._
import play.api.libs.json._
import scala.collection.mutable
import scala.xml.transform.RuleTransformer
import scala.xml.{Node, Elem}

object QtiTransformer {

  def transform(qti:Elem) : (Node,JsValue) = {

    val components : mutable.Map[String,JsObject] = new mutable.HashMap[String,JsObject]()

    val transformedHtml = new RuleTransformer(
      new ChoiceInteractionTransformer(components, qti),
      new TextEntryInteractionTransformer(components, qti),
      new DragAndDropInteractionTransformer(components, qti),
      new OrderInteractionTransformer(components, qti),
      FoldableInteractionTransformer,
      CoverflowInteractionTransformer,
      new FeedbackBlockTransformer(components, qti)
    ).transform(qti)

    val html = (transformedHtml(0) \ "itemBody")(0)

    (html, JsObject(components.toSeq))
  }
}


