package models.itemSession

import qti.models.{ResponseDeclaration, QtiItem}
import qti.models.QtiItem.Correctness
import qti.models.interactions.Interaction

object Score {

  private def getInteraction(qti:QtiItem, id:String) : Option[Interaction] = qti.itemBody.interactions.find( _.responseIdentifier == id)


  def scoreResponses(responses: Seq[ItemResponse], qti: QtiItem, addReport : Boolean = true): Seq[ItemResponse] = {

    def addResponseOutcome(r: ItemResponse): ItemResponse = {

      val outcome : Option[ItemResponseOutcome] = getInteraction(qti,r.id) match {
        case Some(interaction) => {
          val declaration : Option[ResponseDeclaration] = qti.responseDeclarations.find(_.identifier == r.id)
          interaction.getOutcome(declaration, r)
        }
        case _ => None
      }
      ItemResponse(r, outcome)
    }
    responses.map(addResponseOutcome)
  }

  def getMaxScore(qti : QtiItem ) : Int = {
    qti.itemBody.interactions.filter(_.isScoreable).length
  }

}
