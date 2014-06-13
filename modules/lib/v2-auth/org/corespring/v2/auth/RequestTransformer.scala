package org.corespring.v2.auth

import play.api.mvc.{RequestHeader, Request}

private[api] trait CoreTransformer[A,B] {
  def apply(rh:RequestHeader) : Option[B]
}

trait RequestTransformer[A, B <: Request[A]] extends CoreTransformer[A,B]{
  /** Turn a request into a different type of request */
  def apply(rh: Request[A]): Option[B]
}
