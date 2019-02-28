package org.corespring.play.utils

import play.api.GlobalSettings
import play.api.mvc.Controller
import scala.collection._

/**
 * Finds implementations of traits from a given Seq[Controller]
 */
trait ControllerInstanceResolver extends GlobalSettings {

  def controllers: Seq[Controller]

  override def getControllerInstance[A](controllerClass: Class[A]): A = {

    import org.apache.commons.lang3.reflect.TypeUtils

    controllers.find(c => TypeUtils.isInstance(c, controllerClass))
      .map(c => c.asInstanceOf[A])
      .getOrElse(throw new RuntimeException(s"Can't find controller of type: ${controllerClass.getName}"))
  }
}
