package org.corespring.common.log

import org.slf4j.LoggerFactory


trait Logging {

  protected def loggerName: String

  protected lazy val logger = LoggerFactory.getLogger(loggerName)
}

trait PackageLogging extends Logging {

  override protected def loggerName: String = {
    val clazz = this.getClass
    val p = clazz.getPackage
    if (p == null)
      clazz.getName
    else
      p.getName
  }
}

trait ClassLogging extends Logging {
  override protected def loggerName: String = this.getClass.getSimpleName.replace("$", "")
}
