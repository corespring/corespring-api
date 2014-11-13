package org.corespring.qtiToV2.kds.interactions

import org.corespring.qtiToV2.interactions.InteractionTransformer
import play.api.libs.json._
import scala.xml._

object MatchInteractionTransformer extends InteractionTransformer {

  val DefaultCornerText = ""

  override def interactionJs(qti: Node): Map[String, JsObject] = (qti \\ "matchInteraction").map(implicit node => {
    (node \ "@responseIdentifier").text -> Json.obj(
      "componentType" -> "corespring-match",
      "correctResponse" -> answers(qti)(node),
      "model" -> Json.obj(
        "columns" -> (Json.obj("labelHtml" -> ((node \ "cornerText").text.toString match {
          case empty if (empty.isEmpty) => DefaultCornerText
          case nonEmpty: String => nonEmpty
        })) +: columns.values.map(text => Json.obj("labelHtml" -> text)).toSeq),
        "rows" -> rows.map { case(id, text) => Json.obj("id" -> id, "labelHtml" -> text) }.toSeq,
        "answerType" -> "YES_NO"
      )
    )
  }).toMap

  private def columns(implicit node: Node) = filter("Col.*", (choices, acc) => choices.size > acc.size)
  private def rows(implicit node: Node) = filter("Row.*", (choices, acc) => choices.size <= acc.size)
  private def answers(qti: Node)(implicit node: Node) = {
    (qti \\ "responseDeclaration").find(rd => (rd \ "@identifier").text == (node \ "@responseIdentifier").text)
      .map(rd => (rd \ "correctResponse" \ "value").toSeq.map(_.text)).getOrElse(Seq.empty)
      .foldLeft(Map.empty[String, Seq[String]]){ case (acc, text) => text.split(" ") match {
        case Array(one, two) => acc.get(one) match {
          case Some(list) => acc + (one -> (list :+ two))
          case _ => acc + (one -> Seq(two))
        }
        case _ => throw new IllegalArgumentException("Whoa")
      }}.map{ case (row, cols) => Json.obj("id" -> row, "matchSet" -> columns.keySet.map(cols.contains(_))) }.toSeq
  }

  private def filter(regex: String, comparator: (Seq[Node], Seq[Node]) => Boolean)(implicit node: Node) =
    (node \ "simpleMatchSet").find(matchSet =>
      (matchSet \ "simpleAssociableChoice").find(choice => regexMatch(regex, (choice \ "@identifier").text)).nonEmpty
    ).map(_ \ "simpleAssociableChoice").getOrElse((node \ "simpleMatchSet").foldLeft(Seq.empty[Node])((acc, n) =>
      (n \ "simpleAssociableChoice") match {
        case choices if comparator(choices, acc) => choices
        case _ => acc
      }
    )).map(choice => (choice \ "@identifier").text -> choice.text).toMap

  private def regexMatch(regex: String, string: String) = {
    val regexX = regex.r
    string match {
      case regexX(_*) => true
      case _ => false
    }
  }


}
