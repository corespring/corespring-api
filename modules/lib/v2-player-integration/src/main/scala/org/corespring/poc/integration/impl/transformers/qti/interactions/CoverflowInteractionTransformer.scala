package org.corespring.poc.integration.impl.transformers.qti.interactions

import scala.xml.Elem

object CoverflowInteractionTransformer extends NodeReplacementTransformer {

  def labelToReplace: String = "coverflow"
  def replacementNode: Elem = <corespring-coverflow></corespring-coverflow>

}
