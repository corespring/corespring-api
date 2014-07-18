package org.corespring.qtiToV2.interactions

import scala.xml._

import play.api.libs.json._
object OrderInteractionTransformer extends InteractionTransformer {

  override def interactionJs(qti: Node) = (qti \\ "orderInteraction").map(implicit node => {
    val responses = (responseDeclaration(node, qti) \ "correctResponse" \\ "value").map(_.text)
    val identifier = (node \ "@responseIdentifier").text
    val prompt = ((node \ "prompt") match {
      case seq: Seq[Node] if seq.isEmpty => ""
      case seq: Seq[Node] => seq.head.child.mkString
    })

    identifier -> partialObj(
      "componentType" ->
        Some(JsString(if (isPlacementOrdering(node)) "corespring-placement-ordering" else "corespring-ordering")),
      "correctResponse" -> Some(JsArray(responses.map(JsString(_)))),
      "feedback" -> (
        if (isPlacementOrdering(node)) Some(Json.obj(
          "correctFeedbackType" -> "default",
          "partialFeedbackType" -> "default",
          "incorrectFeedbackType" -> "default"
        ))
        else None
      ),
      "model" -> Some(partialObj(
        "prompt" -> Some(JsString(prompt)),
        "config" -> Some(partialObj(
          "shuffle" -> Some(JsBoolean((node \\ "@shuffle").text == "true")),
          "choiceAreaLayout" -> (
            if (isPlacementOrdering(node) && (node \\ "@orientation").text.equalsIgnoreCase("horizontal"))
              Some(JsString("horizontal"))
            else Some(JsString("vertical"))
           ),
          "choiceAreaLabel" -> (
            if (isPlacementOrdering(node)) Some(JsString(prompt)) else None
           ),
          "answerAreaLabel" -> (
            if (isPlacementOrdering(node)) Some(JsString("Place answers here")) else None
           )
        )),
        "choices" -> Some(JsArray((node \\ "simpleChoice")
          .map(choice => Json.obj(
            "label" -> choice.child.filter(_.label != "feedbackInline").mkString.trim,
            "value" -> (choice \ "@identifier").text,
            "content" -> choice.child.filter(_.label != "feedbackInline").mkString.trim,
            "id" -> (choice \ "@identifier").text)
          ))),
        "correctResponse" -> Some(JsArray(responses.map(JsString(_)))),
        "feedback" -> (if (isPlacementOrdering(node)) None else Some(feedback(node, qti)))
      ))
    )
  }).toMap

  override def transform(node: Node): Seq[Node] = node match {
    case e: Elem if e.label == "orderInteraction" => {
      val identifier = (e \ "@responseIdentifier").text
      isPlacementOrdering(node) match {
        case true => <corespring-placement-ordering id={ identifier }></corespring-placement-ordering>
        case _ => <corespring-ordering id={ identifier }></corespring-ordering>
      }
    }
    case _ => node
  }

  private def isPlacementOrdering(node: Node) = (node \ "@csOrderingType").text.equalsIgnoreCase("placement")

}
