package org.corespring.services.salat

import com.novus.salat.dao.SalatDAO

private[salat] trait HasDao[A <: AnyRef, ID <: Any] {

  import com.novus.salat._

  def dao: SalatDAO[A, ID]
  implicit def context: Context
}
