package org.corespring.models.item

case class Workflow(setup: Boolean = false,
  tagged: Boolean = false,
  standardsAligned: Boolean = false,
  qaReview: Boolean = false)

