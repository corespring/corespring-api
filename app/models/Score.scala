package models

import qti.models.QtiItem
import qti.models.QtiItem.Correctness

object Score {

  def scoreResponses(responses: Seq[ItemResponse], qti: QtiItem, addReport : Boolean = true): Seq[ItemResponse] = {

    def addResponseOutcome(r: ItemResponse): ItemResponse = {
      val outcome:Option[ItemResponseOutcome] = qti.itemBody.interactions.find(i => {
        i.responseIdentifier == r.id
      }) match {
        case Some(i) => i.getOutcome(qti.responseDeclarations.find(_.identifier == r.id), r)
        case None => None
      }
      ItemResponse(r, outcome)
    }
    responses.map(addResponseOutcome)
  }

}
