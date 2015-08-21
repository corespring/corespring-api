package org.corespring.v2.player.hooks

import org.corespring.container.client.hooks.Hooks.StatusMessage
import org.corespring.v2.errors.V2Error

import scalaz.Validation

trait ContainerConverters {
  implicit def validationToEither[A](v: Validation[V2Error, A]): Either[StatusMessage, A] = v.leftMap { e => e.statusCode -> e.message }.toEither
  implicit def validationToOption[A](v: Validation[V2Error, A]): Option[StatusMessage] = v.swap.map { e => e.statusCode -> e.message }.toOption
}
