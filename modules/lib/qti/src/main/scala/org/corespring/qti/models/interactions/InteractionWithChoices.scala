package org.corespring.qti.models.interactions

import org.corespring.qti.models.interactions.choices.Choice

trait InteractionWithChoices extends Interaction {
  val choices: Seq[Choice]
  def getChoice(identifier: String): Option[Choice]
}
