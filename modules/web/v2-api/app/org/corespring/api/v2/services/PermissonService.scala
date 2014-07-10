package org.corespring.api.v2.services

import org.corespring.api.v2.errors.V2ApiError

import scalaz.Validation

trait PermissionService[CLIENT, DATA] {
  def create(client: CLIENT, newValue: DATA): Validation[V2ApiError, DATA]
  def get(client: CLIENT, value: DATA): Validation[V2ApiError, DATA]
}
