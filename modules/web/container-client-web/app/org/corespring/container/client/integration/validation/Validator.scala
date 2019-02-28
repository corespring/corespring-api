package org.corespring.container.client.integration.validation

import play.api.{ Mode, Play }

object Validator {

  def absolutePathInProdMode(p: String): Either[String, Boolean] = {
    lazy val msg = s"The component path ($p) is relative - this can cause unpredictable behaviour when running in Prod Mode. see: https://github.com/playframework/playframework/issues/2411"
    if (Play.current.mode == Mode.Prod) {
      if (p.startsWith("/")) Right(true) else Left(msg)
    } else Right(true)
  }
}
