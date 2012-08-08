package controllers

import play.api.Play
import play.api.Play.current
import play.api.Logger

object Log {
  val enabled = true;
  lazy val herokuEnabled: Boolean = try {
    System.getenv("ON_HEROKU")
    true
  } catch {
    case e: NullPointerException => false
    case e: SecurityException => false
  }
  private val herokuInfo = "INFO: ";
  private val herokuDebug = "DEBUG: ";
  private val herokuWarn = "WARN: ";
  private val herokuFatal = "FATAL: ";

  def i(out: String) = {
    if (herokuEnabled) {
      println(herokuInfo + out)
    } else if (Play.isDev && enabled) {
      Logger.info(out)
    } else if (Play.isTest && enabled) {
      println(out)
    }
  }

  def d(out: String) = {
    if (herokuEnabled) {
      println(herokuDebug + out)
    } else if (Play.isDev && enabled) {
      Logger.debug(out)
    } else if (Play.isTest && enabled) {
      println(out)
    }
  }

  def w(out: String) = {
    if (herokuEnabled) {
      println(herokuWarn + out)
    } else if (Play.isDev && enabled) {
      Logger.warn(out)
    } else if (Play.isTest && enabled) {
      println(out)
    }
  }

  def e(out: String) = {
    if (herokuEnabled) {
      println(herokuFatal + out)
    } else if (Play.isDev && enabled) {
      Logger.error(out)
    } else if (Play.isTest && enabled) {
      println(out)
    }
  }

  def f(out: String) = {
    if (herokuEnabled) {
      println(herokuFatal + out)
    } else if (Play.isDev && enabled) {
      Logger.error(out)
    } else if (Play.isTest && enabled) {
      println(out)
    }
    Thread.currentThread().getStackTrace.map(ste => Log.e(ste.toString))
  }

  def u(logType: LogType.LogType, msg: String) {
    logType match {
      case LogType.printFatal => Log.f(msg)
      case LogType.printError => Log.e(msg)
      case LogType.printWarning => Log.w(msg)
      case LogType.printDebug => Log.d(msg)
      case LogType.printInfo => Log.i(msg)
      case _ =>
    }
  }
}

object LogType extends Enumeration {
  type LogType = Value
  val printFatal, printError, printWarning, printDebug, printInfo, printNone = Value
}
