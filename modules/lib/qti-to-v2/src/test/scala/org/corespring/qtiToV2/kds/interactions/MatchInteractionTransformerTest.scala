package org.corespring.qtiToV2.kds.interactions

import org.corespring.qtiToV2.transformers.ItemTransformer
import org.specs2.mutable.Specification
import play.api.libs.json._

class MatchInteractionTransformerTest extends Specification {

  "MatchInteractionTransformer" should {

    val identifier = "RESPONSE1"
    val cornerText: String = "Statement"
    val columns: Map[String, String] = Map("Col1" -> "Yes", "Col2" -> "No")
    val choices = Map("Row1" -> "3 times 7", "Row2" -> "4 times 4")
    val answers: Map[String, Seq[String]] = choices.keys.zip(columns.keys).map { case (a, b) => a -> (Seq(b)) }.toMap

    def qti(identifier: String = identifier,
      cornerText: String = cornerText,
      columns: Map[String, String] = columns,
      choices: Map[String, String] = choices,
      answers: Map[String, Seq[String]] = answers) =
      <assessmentItem>
        <responseDeclaration identifier={ identifier } cardinality="multiple" baseType="directedPair">
          <correctResponse>{ answers.map { case (one, seq) => seq.map(two => <value>{ s"$one $two" }</value>) }.flatten }</correctResponse>
        </responseDeclaration>
        <itemBody>
          <matchInteraction responseIdentifier={ identifier } shuffle="false" maxAssociations="0">
            <cornerText>{ cornerText }</cornerText>
            <simpleMatchSet>
              { choices.map { case (id, choice) => <simpleAssociableChoice identifier={ id } matchMax="0">{ choice }</simpleAssociableChoice> } }
            </simpleMatchSet>
            <simpleMatchSet>
              { columns.map { case (id, column) => <simpleAssociableChoice identifier={ id } matchMax="0">{ column }</simpleAssociableChoice> } }
            </simpleMatchSet>
          </matchInteraction>
        </itemBody>
        <responseProcessing template="http://www.imsglobal.org/question/qti_v2p1/rptemplates/match_correct"/>
      </assessmentItem>

    val result = MatchInteractionTransformer.interactionJs(qti(), ItemTransformer.EmptyManifest).get(identifier)
      .getOrElse(throw new Exception("Result did not contain interaction"))

    "transform rows" in {
      (result \ "model" \ "rows").as[Seq[JsObject]]
        .map(row => (row \ "id").as[String] -> (row \ "labelHtml").as[String]).toMap must be equalTo (choices)
    }

    "transform columns" in {
      (result \ "model" \ "columns").as[Seq[JsObject]]
        .map(column => (column \ "labelHtml").as[String]) must be equalTo (cornerText +: columns.values.toSeq)
    }

    "transform answers" in {
      (result \ "correctResponse").as[Seq[JsObject]]
        .map(r => (r \ "id").as[String] -> columns.keySet.zipWithIndex
          .filter { case (col, index) => val bools = ((r \ "matchSet").as[Seq[Boolean]]); bools(index) }
          .map(_._1).toSeq).toMap must be equalTo (answers)
    }

  }

}
