package org.corespring.clientlogging

import scalaz._
import Scalaz._
import org.slf4j._
import collection.JavaConversions._
import collection.JavaConverters._
import org.corespring.common.log.PackageLogging
import java.util.Date


class ClientLogEntry(logname: String, end: Date = new Date()) extends LogEntryTemplate{
  val name = logname
  def toString(message:String,messageType:MessageType.MessageType, extraargs:Seq[String] = Seq()) = Seq(
    s"\n***Client Log Entry***",
    s"${messageType}: ${message}\n",
    s"${extraargs.mkString("\n")}",
    s"***End ${end.toString}***"
  ).mkString("\n")
}
trait ClientLogging extends PackageLogging{
  override lazy val logger = new LogEntryLogger(new ClientLogEntry(loggerName))
}
