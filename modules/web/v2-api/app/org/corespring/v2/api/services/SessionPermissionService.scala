package org.corespring.v2.api.services

import org.corespring.platform.core.models.Organization
import org.corespring.v2.errors.Errors.notReady
import org.corespring.v2.errors.V2Error
import play.api.libs.json.JsValue

import scalaz.{ Failure, Validation }

trait SessionPermissionService extends PermissionService[Organization, JsValue] {
  override def create(client: Organization, newValue: JsValue): Validation[V2Error, JsValue] = Failure(notReady)

  override def get(client: Organization, value: JsValue): Validation[V2Error, JsValue] = Failure(notReady)
}
