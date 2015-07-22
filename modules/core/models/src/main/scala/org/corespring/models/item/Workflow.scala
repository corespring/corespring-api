package org.corespring.models.item

case class Workflow(setup: Boolean = false,
  tagged: Boolean = false,
  standardsAligned: Boolean = false,
  qaReview: Boolean = false)

object Workflow {
  object Keys {
    val setup: String = "setup"
    val tagged: String = "tagged"
    val standardsAligned: String = "standardsAligned"
    val qaReview: String = "qaReview"
  }
}