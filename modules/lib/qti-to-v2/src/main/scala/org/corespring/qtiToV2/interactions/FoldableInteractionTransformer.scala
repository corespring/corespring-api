package org.corespring.qtiToV2.interactions

import scala.xml.Elem

object FoldableInteractionTransformer extends NodeReplacementTransformer {

  def labelToReplace: String = "foldable"
  def replacementNode: Elem = <div corespring-foldable="corespring-foldable"></div>

}
