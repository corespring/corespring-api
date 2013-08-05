package org.corespring.platform.core.models.itemSession

import org.corespring.qti.models.{ResponseDeclaration, QtiItem}
import org.corespring.qti.models.interactions.Interaction
import org.corespring.qti.models.responses.{ResponseOutcome, Response}

object Score {

  private def getInteraction(qti:QtiItem, id:String) : Option[Interaction] = qti.itemBody.interactions.find( _.responseIdentifier == id)


  def scoreResponses(responses: Seq[Response], qti: QtiItem, addReport : Boolean = true): Seq[Response] = {

    def addResponseOutcome(r: Response): Response = {

      val outcome : Option[ResponseOutcome] = getInteraction(qti,r.id) match {
        case Some(interaction) => {
          val declaration : Option[ResponseDeclaration] = qti.responseDeclarations.find(_.identifier == r.id)
          interaction.getOutcome(declaration, r)
        }
        case _ => None
      }
      Response.combine(r, outcome)
    }
    responses.map(addResponseOutcome)
  }

  def getMaxScore(qti : QtiItem ) : Int = {
    qti.itemBody.interactions.filter(_.isScoreable).length
  }

}
