package org.corespring.v2.log

import play.api.{ Logger, LoggerLike }

private[v2] object V2LoggerFactory {

  def getLogger(names: String*): LoggerLike = Logger(("org.corespring.v2" +: names).mkString("."))

}
