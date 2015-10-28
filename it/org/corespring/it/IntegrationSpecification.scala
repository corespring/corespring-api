package org.corespring.it

import akka.util.Timeout
import bootstrap.Main
import grizzled.slf4j.Logger
import org.corespring.itemSearch.ItemIndexDeleteService
import org.specs2.execute.{ Result, AsResult, Results }
import org.specs2.mutable.Around
import play.api.test._

import scala.concurrent.Await
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

  protected def removeData() = {
    logger.debug(s"function=removeData - dropping collections")
    Main.db.collectionNames.filterNot(_.contains("system")).foreach { n =>
      Main.db(n).dropCollection()
    }
  }

  override def around[T](t: => T)(implicit evidence$1: AsResult[T]): Result = {
    removeData()
    AsResult(t)
  }
}

trait ItemIndexCleaner {

  lazy val logger: grizzled.slf4j.Logger = Logger(this.getClass)

  def cleanIndex() = {
    logger.info("cleaning item index...")
    Await.result(
      bootstrap.Main.itemIndexService.asInstanceOf[ItemIndexDeleteService].delete(),
      1.second)
  }
}

