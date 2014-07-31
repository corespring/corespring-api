package org.corespring.v2.log

import play.api.{ Logger, LoggerLike }

private[v2] object V2LoggerFactory {

  def getLogger(names: String*): LoggerLike = {
    val n = names match {
      case Nil => "org.corespring.v2"
      case _ => s"org.corespring.v2.${names.mkString(".")}"
    }
    Logger(n)
  }
}
