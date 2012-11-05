package models

import qti.models.QtiItem
import qti.models.QtiItem.Correctness

object Score {

  def scoreResponses(responses: Seq[ItemResponse], qti: QtiItem, addReport : Boolean = true): Seq[ItemResponse] = {

    def addResponseOutcome(r: ItemResponse): ItemResponse = {
      val report : Map[String,Boolean] = if (addReport) makeReport(r, qti) else Map()
      r match {
        case StringItemResponse(id, value, _) => {
          val outcome = correctnessToOutcome(qti.isCorrect(id, value), report)
          ItemResponse(r, outcome)
        }
        case ArrayItemResponse(id, value, _) => {
          val outcome = correctnessToOutcome(qti.isCorrect(id, value.mkString(",")), report)
          ItemResponse(r, outcome)
        }
      }
    }
    responses.map(addResponseOutcome)
  }

  private def makeReport(ir: ItemResponse, qti: QtiItem): Map[String, Boolean] = {

    ir.getIdValueIndex.map((fvi: (String, String, Int)) => {
      val (f, v, i) = fvi
      (v, qti.isValueCorrect(f, v, i))
    }).toMap
  }

  private def correctnessToOutcome(c: Correctness.Value, report: Map[String, Boolean]): Option[ItemResponseOutcome] = c match {
    case Correctness.Correct => Some(ItemResponseOutcome(1, report = report))
    case Correctness.Incorrect => Some(ItemResponseOutcome(0, report = report))
    case _ => None
  }


}
