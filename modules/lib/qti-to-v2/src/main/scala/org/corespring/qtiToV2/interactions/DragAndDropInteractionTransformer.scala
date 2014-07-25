package org.corespring.qtiToV2.interactions

import scala.xml._
import scala.xml.transform._

import play.api.libs.json._

object DragAndDropInteractionTransformer extends InteractionTransformer with NodeUtils {

  private object AnswerAreaTransformer extends RewriteRule {

    private def landingPlace(elem: Elem): Node = {
      elem.copy(label = "span",
        attributes = (new UnprefixedAttribute("landing-place", "landing-place", elem.attributes.toSeq.head)
          ++ elem.attributes).fold(Null)((soFar, attr) => {
            attr.key match {
              case "identifier" => soFar append new UnprefixedAttribute("id", attr.value, soFar.tail.last)
              case _ => soFar append attr
            }
          }))
    }

    override def transform(node: Node): Seq[Node] = node match {
      case e: Elem if e.label == "landingPlace" => landingPlace(e)
      case _ => node
    }

  }

  object Defaults {
    val shuffle = false
    val expandHorizontal = false
  }

  override def interactionJs(qti: Node) = (qti \\ "dragAndDropInteraction").map(node => {
    (node \\ "@responseIdentifier").text -> Json.obj(
      "componentType" -> "corespring-drag-and-drop",
      "correctResponse" -> JsObject(
        (responseDeclaration(node, qti) \ "correctResponse" \ "value").map(valueNode => {
          ((valueNode \ "@identifier").text -> JsArray((valueNode \ "value").map(v => JsString(v.text.trim))))
        })),
      "model" -> partialObj(
        "choices" -> Some(JsArray((node \\ "draggableChoice").map(n =>
          Json.obj(
            "id" -> (n \ "@identifier").text,
            "content" -> n.child.map(clearNamespace).mkString)))),
        "answerArea" -> ((node \\ "answerArea") match {
          case empty: Seq[Node] if empty.isEmpty => None
          case _ => Some(JsString(
            new RuleTransformer(AnswerAreaTransformer).transform((node \\ "answerArea").head)
              .head.child.map(clearNamespace).mkString))
        }),
        "config" -> Some(partialObj(
          "shuffle" -> Some(JsBoolean((node \ "draggableChoiceGroup") match {
            case choiceGroups: Seq[Node] if choiceGroups.nonEmpty =>
              choiceGroups.find(g => (g \ "@shuffle").text.nonEmpty) match {
                case Some(choiceGroup) => (choiceGroup \ "@shuffle").text == "true"
                case _ => Defaults.shuffle
              }
            case _ => Defaults.shuffle
          })),
          "expandHorizontal" -> Some(JsBoolean(Defaults.expandHorizontal)),
          "itemsPerRow" -> ((node \ "draggableChoiceGroup") match {
            case choiceGroups: Seq[Node] if choiceGroups.nonEmpty =>
              choiceGroups.find(g => (g \ "@itemsPerRow").text.nonEmpty) match {
                case Some(choiceGroup) => Some(JsNumber((choiceGroup \ "@itemsPerRow").text.toInt))
                case _ => None
              }
            case _ => None
          }),
          "choicesPosition" -> (whichFirst(qti, "draggableChoice", "landingPlace") match {
            case Some("draggableChoice") => Some(JsString("above"))
            case Some(_) => Some(JsString("below"))
            case None => None
          })))),
      "feedback" -> feedback(node, qti))
  }).toMap

  override def transform(node: Node): Seq[Node] = node match {
    case e: Elem if e.label == "dragAndDropInteraction" => {
      val identifier = (e \ "@responseIdentifier").text
      <corespring-drag-and-drop id={ identifier }></corespring-drag-and-drop>.withPrompt(node)
    }
    case _ => node
  }

}
