package org.corespring.poc.integration.impl.transformers.qti

import org.corespring.poc.integration.impl.transformers.qti.interactions.{FeedbackBlockTransformer, TextEntryInteractionTransformer, ChoiceInteractionTransformer}
import play.api.libs.json.{Json, JsObject, JsValue}
import scala.collection.mutable
import scala.xml.transform.RuleTransformer
import scala.xml.{Node, Elem}

object QtiTransformer {

  def transform(qti:Elem) : (Node,JsValue) = {

    val components : mutable.Map[String,JsObject] = new mutable.HashMap[String,JsObject]()

    val transformedHtml = new RuleTransformer(
      new ChoiceInteractionTransformer(components, qti),
      new TextEntryInteractionTransformer(components, qti),
      new FeedbackBlockTransformer(components, qti)
    ).transform(qti)

    val html = (transformedHtml(0) \ "itemBody")(0)

    println(transformedHtml)
    println(Json.prettyPrint(JsObject(components.toSeq)))

    (html, JsObject(components.toSeq))
  }
}


