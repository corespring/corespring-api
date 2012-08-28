package controllers.testplayer.qti

import scala.xml.{NodeSeq, Node}

class ResponseDeclaration(rootElement: Node) {

  if (!"responseDeclaration".equals(rootElement.label)) {
    throw new IllegalArgumentException("ResponseDeclaration must be created from a <responseDeclaration/> node.")
  }

  val identifier: String = (rootElement \ "@identifier").text

  // Enum for this?
  val cardinality: String = (rootElement \ "@cardinality").text.toLowerCase

  private val correctResponse = cardinality match {
    case "multiple" => (rootElement \ "correctResponse" \ "value").map(_.text)
    case _ => (rootElement \ "correctResponse" \ "value").text
  }

  val mapping: Option[Mapping] = cardinality match {
    case "multiple" => {
      (rootElement \ "mapping") match {
        case NodeSeq.Empty => None
        case mappingElement: NodeSeq =>
          Some(Mapping(mappingElement.head, (mappingElement \ "@defaultValue").text.toInt))
      }
    }
    case _ => None
  }

  def responseFor(choiceIdentifier: String): String = {
    if (cardinality equals "multiple") {
      responseFor(List(choiceIdentifier))
    }
    else if (choiceIdentifier equals correctResponse) "1" else "0"
  }

  /**
   * If multiple cardinality with mapping, looks up all choice identifiers in mapping, sums results and returns as
   * string.
   */
  def responseFor(choiceIdentifiers: Seq[String]): String = {
    if (!(cardinality equals "multiple")) {
      throw new IllegalArgumentException("Cannot evaluate multiple identifiers for single cardinality ResponseDeclaration")
    }
    mapping match {
      case None => ""
      case Some(mapping: Mapping) => {
        if (choiceIdentifiers.isEmpty) mapping.defaultValue.toString
          else choiceIdentifiers.foldLeft(0)(_ + mapping.get(_)).toString
      }
    }
  }

  /**
   * Helper class for <responseDeclaration>'s <mapping> element. When called with get, provides <mapping>'s defaultValue
   * attribute if no corresponding <mapEntry> present.
   */
  case class Mapping(mappingElement: Node, defaultValue: Int = 0) {

    val map = (mappingElement \ "mapEntry").map(
      mapEntry => ((mapEntry \\ "@mapKey").text, (mapEntry \\ "@mappedValue").text.toInt)).toMap

    def get(key: String): Int = map.getOrElse(key, defaultValue)
  }

}
