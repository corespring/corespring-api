package org.corespring.common.log

import play.api.LoggerLike


trait Logging {

  protected def loggerName : String

  protected lazy val Logger : LoggerLike = play.api.Logger(loggerName)
}

trait PackageLogging extends Logging {

  override protected def loggerName : String = {
    val clazz = this.getClass
    val p = clazz.getPackage
    if(p == null)
      clazz.getName
    else
      p.getName
  }

}

trait ClassLogging extends Logging{
  override protected def loggerName : String = this.getClass.getSimpleName.replace("$", "")
}
