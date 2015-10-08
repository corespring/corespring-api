package org.corespring.qtiToV2.interactions

import scala.xml._

object CorespringTabTransformer extends InteractionTransformer {

  /* Tabs don't have a JSON component representation */
  override def interactionJs(qti: Node, manifest: Node) = Map.empty

  override def transform(node: Node, manifest: Node): Seq[Node] = node match {
    case e: Elem if (e.label == "cs-tabs") => e.copy(label = "corespring-tabs")
    case e: Elem if (e.label == "cs-tab") => e.copy(label = "corespring-tab")
    case _ => node
  }

}
