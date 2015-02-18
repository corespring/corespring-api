package org.corespring.qtiToV2.kds.interactions

import org.corespring.qtiToV2.interactions.InteractionTransformer
import play.api.libs.json._

import scala.xml._

case class SelectPointInteractionTransformer(qti: Node) extends InteractionTransformer {

  private object InteractionType extends Enumeration {
    type InteractionType = Value
    val Points, Line, Unknown = Value
  }

  import InteractionType._

  /**
   * Determine whether the QTI interaction provided by KDS is intended to be a line interaction or a set of points.
   */
  private def getType(node: Node): InteractionType = {

    /**
     * Return true if the provided nodes represent a set of points, false otherwise. In the case of KDS, this means
     * that each Node must be a <value/> with defined xcoordinate and ycoordinate attributes.
     */
    def valuesArePoints(values: Seq[Node]) = {
      def valueIsPoint(value: Node) = value.label == "value" &&
        (value \ "@xcoordinate").nonEmpty && (value \ "@ycoordinate").nonEmpty
      values.find(value => !valueIsPoint(value)).isEmpty
    }

    /**
     * Return true if the provided nodes represents a single <value/> with the required attributes defined.
     */
    def valuesAreLine(values: Seq[Node]) = {
      val requiredAttributes = Seq("startPointXCoordinate", "startPointYCoordinate", "startPointConsider",
        "startPointTolerance", "endPointXCoordinate", "endPointYCoordinate", "endPointConsider", "endPointTolerance",
        "xIntercept", "xInterceptConsider", "xInterceptTolerance", "yIntercept", "yInterceptConsider",
        "yInterceptTolerance", "slope", "slopeConsider", "slopeTolerance")
      values.length match {
        case 1 => requiredAttributes.find(attribute => values.head.attribute(attribute).isEmpty).isEmpty
        case _ => false
      }
    }

    val values = (responseDeclaration(node, qti) \ "correctResponse" \ "value").toSeq

    values match {
      case _ if (valuesArePoints(values)) => Points
      case _ if (valuesAreLine(values)) => Line
      case _ => Unknown
    }

  }

  override def transform(node: Node) = node.label match {
    case "selectPointInteraction" => getType(node) match {
      case Points => PointsInteractionTransformer.transform(node)
      case Line => LineInteractionTransformer.transform(node)
      case _ => throw new IllegalArgumentException(s"$node does not represent any known KDS format")
    }
    case _ => node
  }

  override def interactionJs(node: Node) =
    PointsInteractionTransformer.interactionJs(node) ++ LineInteractionTransformer.interactionJs(node)

  private object LineInteractionTransformer extends InteractionTransformer {

    override def transform(node: Node) = ???

    override def interactionJs(qti: Node) = (qti \\ "selectPointInteraction").map(implicit node => {
      getType(node) match {
        case Line => {
          // TODO
          None
        }
        case _ => None
      }
    }).flatten.toMap

  }

  private object PointsInteractionTransformer extends InteractionTransformer {

    override def transform(node: Node) = {
      val identifier = (node \ "@responseIdentifier").text
      node match {
        case elem: Elem if (node.label == "selectPointInteraction") =>
          elem.child.filter(_.label != "object").map(n => n.label match {
            case "prompt" => <p class="prompt">{n.child}</p>
            case _ => n
          }) ++ <corespring-point-intercept id={identifier}></corespring-point-intercept>
        case _ => node
      }
    }

    override def interactionJs(qti: Node) = (qti \\ "selectPointInteraction").map(implicit node => {
      getType(node) match {
        case Points => {
          Some((node \ "@responseIdentifier").text -> Json.obj(
            "componentType" -> "corespring-point-intercept",
            "correctResponse" -> answers(qti)(node),
            "model" -> Json.obj(
              "config" -> partialObj(
                "domainLabel" -> property("xAxisTitle").map(JsString(_)),
                "rangeLabel" -> property("yAxisTitle").map(JsString(_)),
                "graphWidth" -> property("gridWidthInPixels").map(JsString(_)),
                "graphHeight" -> property("gridHeightInPixels").map(JsString(_)),
                "domain" -> property("xAxisMaxValue").map(f => JsNumber(f.toInt)),
                "range" -> property("yAxisMaxValue").map(f => JsNumber(f.toInt)),
                "maxPoints" -> Some(JsNumber((node \ "@maxChoices").text.toInt))
              )
            )
          ))
        }
        case _ => None
      }
    }).flatten.toMap

    private def answers(qti: Node)(implicit node: Node) =
      (responseDeclaration(node, qti) \ "correctResponse" \ "value").toSeq
        .map(v =>  s"${(v \ "@xcoordinate").text},${(v \ "@ycoordinate").text}")

    private def property(name: String)(implicit node: Node): Option[String] =
      (node \ "object" \ "param").toSeq.find(p => (p \ "@name").text == name).map(p => (p \ "@value").text)

  }


}
