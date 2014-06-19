package org.corespring.v2.auth.models

object Mode extends Enumeration {
  type Mode = Value
  val gather, view, evaluate = Value
}
