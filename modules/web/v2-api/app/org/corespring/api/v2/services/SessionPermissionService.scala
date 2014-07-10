package org.corespring.api.v2.services

import org.corespring.api.v2.errors.Errors.notReady
import org.corespring.api.v2.errors.V2ApiError
import org.corespring.platform.core.models.Organization
import play.api.libs.json.JsValue

import scalaz.{ Failure, Validation }

trait SessionPermissionService extends PermissionService[Organization, JsValue] {
  override def create(client: Organization, newValue: JsValue): Validation[V2ApiError, JsValue] = Failure(notReady)

  override def get(client: Organization, value: JsValue): Validation[V2ApiError, JsValue] = Failure(notReady)
}
