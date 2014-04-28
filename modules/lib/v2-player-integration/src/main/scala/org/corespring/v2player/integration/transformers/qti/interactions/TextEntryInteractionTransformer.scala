package org.corespring.v2player.integration.transformers.qti.interactions

import scala.xml._
import play.api.libs.json._
import org.corespring.v2player.integration.transformers.qti.interactions.equation.DomainParser
import scala.xml.transform.RuleTransformer

object TextEntryInteractionTransformer extends Transformer {

  def transform(qti: Node): Node =
    new RuleTransformer(new TextEntryInteractionTransformer(qti)).transform(qti).head

}

case class TextEntryInteractionTransformer(qti: Node) extends InteractionTransformer with DomainParser {

  val equationRegex = "eqn[:]?(.*)?".r

  override def interactionJs(qti: Node) = (qti \\ "textEntryInteraction").map(implicit node => {
    val responseDeclarationNode = responseDeclaration(node, qti)
    val correctResponses = (responseDeclarationNode \ "correctResponse" \\ "value").map(_.text).toSet

    (node \ "@responseIdentifier").text -> partialObj(
      "componentType" -> Option(isEquation(node, qti) match {
        case true => JsString("corespring-function-entry")
        case _ => JsString("corespring-text-entry")
      }),
      "correctResponse" -> Option(correctResponses.size match {
        case 0 => equationConfig(responseDeclarationNode) match {
          case Some(config) => config
          case None => Json.arr()
        }
        case 1 => isEquation(node, qti) match {
          case true => equationConfig(responseDeclarationNode).getOrElse(Json.obj()) + ("equation" -> JsString(correctResponses.head))
          case _ => JsString(correctResponses.head)
        }
        case _ => JsArray(correctResponses.map(JsString(_)).toSeq)
      })
    )
  }).toMap

  override def transform(node: Node): Seq[Node] = node match {
    case e: Elem if e.label == "textEntryInteraction" => isEquation(node, qti) match {
      case true => <corespring-function-entry id={(node \ "@responseIdentifier").text}></corespring-function-entry>
      case false => <corespring-text-entry id={(node \ "@responseIdentifier").text}></corespring-text-entry>
    }
    case _ => node
  }

  private def isEquation(node: Node, qti: Node) = {
    val baseType = node.label match {
      case "responseDeclaration" => (node \ "@baseType").text
      case "textEntryInteraction" => (responseDeclaration(node, qti) \ "@baseType").text
      case _ => false
    }

    baseType match {
      case equationRegex(_*) => true
      case "line" => true
      case _ => false
    }
  }

  private def equationConfig(responseDeclaration: Node): Option[JsObject] = {
    (responseDeclaration \ "@baseType").text match {
      case equationRegex(params) if Option(params).isDefined => {
        val values = params.split(" ").map(param => {
          param.split(":") match {
            case Array(key: String, value: String, _*) => Some(key -> (key match {
              case "domain" => parseDomain(value)
              case "sigfigs" => JsNumber(value.toInt)
              case _ => JsString(value)
            }))
            case _ => None
          }
        }).flatten.toSeq
        Some(JsObject(values))
      }
      case _ => None
    }
  }

}
