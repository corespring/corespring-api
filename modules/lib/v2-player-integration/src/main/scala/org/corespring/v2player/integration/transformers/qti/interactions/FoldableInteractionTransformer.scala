package org.corespring.v2player.integration.transformers.qti.interactions

import scala.xml.Elem

object FoldableInteractionTransformer extends NodeReplacementTransformer {

  def labelToReplace: String = "foldable"
  def replacementNode: Elem = <div corespring-foldable="corespring-foldable"/>

}
