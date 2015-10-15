package org.corespring.it

import akka.util.Timeout
import bootstrap.Main
import grizzled.slf4j.Logger
import org.corespring.services.salat.bootstrap.CollectionNames
import org.specs2.execute.{ Result, AsResult, Results }
import org.specs2.mutable.Around
import play.api.test._

import scala.concurrent.duration._

/**
 * Note: We don't make use of BeforeAfterAll as our specs2 version (2.2.1) doesn't have it.
 * Instead we use sbt Test.Setup/Test.Cleanup for the time being.
 */
abstract class IntegrationSpecification
  extends PlaySpecification
  with Results
  with Around {

  sequential

  lazy val logger: grizzled.slf4j.Logger = Logger(this.getClass)

  override implicit def defaultAwaitTimeout: Timeout = 3.seconds

  protected def dropDb() = {
    logger.debug(s"function=dropDb - dropping collections")
    CollectionNames.all.foreach { n =>
      Main.db(n).dropCollection()
    }
  }

  override def around[T](t: => T)(implicit evidence$1: AsResult[T]): Result = {
    logger.debug(s"function=around - dropping db")
    dropDb()
    AsResult(t)
  }
}

