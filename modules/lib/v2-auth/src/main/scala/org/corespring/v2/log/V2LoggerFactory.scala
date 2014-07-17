package org.corespring.v2.log

import org.slf4j.LoggerFactory

private[v2] object V2LoggerFactory {

  def getLogger(names: String*) = {
    val n = names match {
      case Nil => "org.corespring.v2"
      case _ => s"org.corespring.v2.${names.mkString(".")}"
    }
    LoggerFactory.getLogger(n)
  }
}
