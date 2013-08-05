package common.log

import play.api.LoggerLike

trait PackageLogging {

  private lazy val name : String = {
    val clazz = this.getClass
    val p = clazz.getPackage
    if(p == null)
      clazz.getName
    else
      p.getName
  }

  protected lazy val Logger : LoggerLike = play.api.Logger(name)
}
