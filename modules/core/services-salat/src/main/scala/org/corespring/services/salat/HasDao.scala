package org.corespring.services.salat

import salat.dao.SalatDAO

private[salat] trait HasDao[A <: AnyRef, ID <: Any] {

  import salat._

  def dao: SalatDAO[A, ID]
  implicit def context: Context
}
