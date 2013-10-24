package org.corespring.log

import org.slf4j.{Marker, LoggerFactory, Logger}
import scalaz.{Failure, Success, Validation}

sealed class LogEntryLogger[A <: LogEntryTemplate](logEntry: A) extends Logger{
  private lazy val logger = LoggerFactory.getLogger(loggerName)

  protected def loggerName: String = getName

  def getName: String = "Client Logger"

  def isTraceEnabled: Boolean = false

  def trace(msg: String) {}

  def trace(format: String, arg: Any) {}

  def trace(format: String, arg1: Any, arg2: Any) {}

  def trace(format: String, arguments: AnyRef*) {}

  def trace(msg: String, t: Throwable) {}

  def isTraceEnabled(marker: Marker): Boolean = false

  def trace(marker: Marker, msg: String) {}

  def trace(marker: Marker, format: String, arg: Any) {}

  def trace(marker: Marker, format: String, arg1: Any, arg2: Any) {}

  def trace(marker: Marker, format: String, argArray: AnyRef*) {}

  def trace(marker: Marker, msg: String, t: Throwable) {}

  def isDebugEnabled: Boolean = true

  def debug(msg: String) {
    logger.debug(logEntry.toString(msg,MessageType.Debug))
  }
  def debug(format: String, arg: Any) {
    logger.debug(logEntry.toString(format,MessageType.Debug,Seq(arg.toString)))
  }

  def debug(format: String, arg1: Any, arg2: Any) {
    logger.debug(logEntry.toString(format,MessageType.Debug,Seq(arg1.toString,arg2.toString)))
  }

  def debug(format: String, arguments: AnyRef*) {
    logger.debug(logEntry.toString(format,MessageType.Debug,arguments.toSeq.map(_.toString)))
  }

  def debug(msg: String, t: Throwable) {
    logger.debug(logEntry.toString(msg,MessageType.Debug,t.getStackTrace.map(_.toString)))
  }

  def isDebugEnabled(marker: Marker): Boolean = false

  def debug(marker: Marker, msg: String) {}

  def debug(marker: Marker, format: String, arg: Any) {}

  def debug(marker: Marker, format: String, arg1: Any, arg2: Any) {}

  def debug(marker: Marker, format: String, arguments: AnyRef*) {}

  def debug(marker: Marker, msg: String, t: Throwable) {}

  def isInfoEnabled: Boolean = true

  def info(msg: String) {
    logger.info(logEntry.toString(msg,MessageType.Info).toString)
  }

  def info(format: String, arg: Any) {
    logger.info(logEntry.toString(format,MessageType.Info,Seq(arg.toString)))
  }

  def info(format: String, arg1: Any, arg2: Any) {
    logger.info(logEntry.toString(format,MessageType.Info, Seq(arg1.toString,arg2.toString)))
  }

  def info(format: String, arguments: AnyRef*) {
    logger.info(logEntry.toString(format,MessageType.Info,arguments.map(_.toString)))
  }

  def info(msg: String, t: Throwable) {
    logger.info(logEntry.toString(msg,MessageType.Info,t.getStackTrace.map(_.toString)))
  }

  def isInfoEnabled(marker: Marker): Boolean = false

  def info(marker: Marker, msg: String) {}

  def info(marker: Marker, format: String, arg: Any) {}

  def info(marker: Marker, format: String, arg1: Any, arg2: Any) {}

  def info(marker: Marker, format: String, arguments: AnyRef*) {}

  def info(marker: Marker, msg: String, t: Throwable) {}

  def isWarnEnabled: Boolean = true

  def warn(msg: String) {
    logger.warn(logEntry.toString(msg,MessageType.Warn))
  }

  def warn(format: String, arg: Any) {
    logger.warn(logEntry.toString(format,MessageType.Warn, Seq(arg.toString)))
  }

  def warn(format: String, arguments: AnyRef*) {
    logger.warn(logEntry.toString(format,MessageType.Warn,arguments.map(_.toString)))
  }

  def warn(format: String, arg1: Any, arg2: Any) {
    logger.warn(logEntry.toString(format,MessageType.Warn, Seq(arg1.toString,arg2.toString)))
  }

  def warn(msg: String, t: Throwable) {
    logger.warn(logEntry.toString(msg,MessageType.Warn,t.getStackTrace.map(_.toString)))
  }

  def isWarnEnabled(marker: Marker): Boolean = false

  def warn(marker: Marker, msg: String) {}

  def warn(marker: Marker, format: String, arg: Any) {}

  def warn(marker: Marker, format: String, arg1: Any, arg2: Any) {}

  def warn(marker: Marker, format: String, arguments: AnyRef*) {}

  def warn(marker: Marker, msg: String, t: Throwable) {}

  def isErrorEnabled: Boolean = true

  def error(msg: String) {
    logger.error(logEntry.toString(msg,MessageType.Error))
  }

  def error(format: String, arg: Any) {
    logger.error(logEntry.toString(format,MessageType.Error, Seq(arg.toString)))
  }

  def error(format: String, arg1: Any, arg2: Any) {
    logger.error(logEntry.toString(format,MessageType.Error, Seq(arg1.toString,arg2.toString)))
  }

  def error(format: String, arguments: AnyRef*) {
    logger.error(logEntry.toString(format,MessageType.Error, arguments.map(_.toString)))
  }

  def error(msg: String, t: Throwable) {
    logger.error(logEntry.toString(msg,MessageType.Error,t.getStackTrace().map(_.toString)))
  }

  def isErrorEnabled(marker: Marker): Boolean = false

  def error(marker: Marker, msg: String) {}

  def error(marker: Marker, format: String, arg: Any) {}

  def error(marker: Marker, format: String, arg1: Any, arg2: Any) {}

  def error(marker: Marker, format: String, arguments: AnyRef*) {}

  def error(marker: Marker, msg: String, t: Throwable) {}

  def isFatalEnabled: Boolean = true

  def fatal(msg: String) {
    logger.error(logEntry.toString(msg,MessageType.Fatal).toString)
  }

  def fatal(format: String, arg: Any) {
    logger.error(logEntry.toString(format,MessageType.Fatal, Seq(arg.toString)))
  }

  def fatal(format: String, arg1: Any, arg2: Any) {
    logger.error(logEntry.toString(format,MessageType.Fatal, Seq(arg1.toString,arg2.toString)))
  }

  def fatal(format: String, arguments: Array[AnyRef]) {
    logger.error(logEntry.toString(format,MessageType.Fatal, arguments.map(_.toString)))
  }

  def fatal(msg: String, t: Throwable) {
    logger.error(logEntry.toString(msg,MessageType.Fatal,t.getStackTrace().map(_.toString)))
  }
}

object MessageType extends Enumeration{
  type MessageType = Value
  val Fatal, Error, Warn, Info, Debug = Value
}

trait LogEntryTemplate{
  val name:String
  def toString(message:String,messageType:MessageType.MessageType, extraargs:Seq[String] = Seq()):String
}
