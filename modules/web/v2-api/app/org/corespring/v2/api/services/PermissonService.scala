package org.corespring.v2.api.services

import org.corespring.v2.errors.V2Error

import scalaz.Validation

trait PermissionService[CLIENT, DATA] {
  def create(client: CLIENT, newValue: DATA): Validation[V2Error, DATA]
  def get(client: CLIENT, value: DATA): Validation[V2Error, DATA]
}
