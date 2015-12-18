package org.corespring.v2.auth

import org.corespring.models.auth.Permission
import org.corespring.v2.errors.V2Error

import scalaz.Validation

trait Access[DATA, REQUESTER] {
  def grant(identity: REQUESTER, permission: Permission, data: DATA): Validation[V2Error, Boolean]

}
