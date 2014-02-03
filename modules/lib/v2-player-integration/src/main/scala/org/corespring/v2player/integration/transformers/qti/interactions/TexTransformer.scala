package org.corespring.v2player.integration.transformers.qti.interactions

object TexTransformer extends NodeReplacementTransformer {

  def labelToReplace = "tex"
  def replacementNode = <corespring-tex></corespring-tex>

}
