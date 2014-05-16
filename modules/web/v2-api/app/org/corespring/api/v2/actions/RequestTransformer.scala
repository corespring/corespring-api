package org.corespring.api.v2.actions

import play.api.mvc.Request

trait RequestTransformer[A, B <: Request[A]] {
  /** Turn a request into a different type of request */
  def apply(rh: Request[A]): Option[B]
}
