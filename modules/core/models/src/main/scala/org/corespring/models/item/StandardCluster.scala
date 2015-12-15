package org.corespring.models.item

case class StandardCluster(text: String, hidden: Boolean, source: String)

object StandardCluster {
  object Keys {
    val hidden = "hidden"
    val source = "source"
    val text = "text"
  }
}
