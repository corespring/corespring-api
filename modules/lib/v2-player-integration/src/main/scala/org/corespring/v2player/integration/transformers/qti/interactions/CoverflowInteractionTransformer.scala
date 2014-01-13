package org.corespring.v2player.integration.transformers.qti.interactions

import scala.xml.Elem

object CoverflowInteractionTransformer extends NodeReplacementTransformer {

  def labelToReplace: String = "coverflow"
  def replacementNode: Elem = <corespring-coverflow></corespring-coverflow>

}
