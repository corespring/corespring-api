package org.corespring.it

import akka.util.Timeout
import grizzled.slf4j.Logger
import org.specs2.execute.Results
import play.api.test._

import scala.concurrent.duration._

/**
 * Note: We don't make use of BeforeAfterAll as our specs2 version (2.2.1) doesn't have it.
 * Instead we use sbt Test.Setup/Test.Cleanup for the time being.
 */
abstract class IntegrationSpecification
  extends PlaySpecification
  with Results {

  sequential

  protected def logger: grizzled.slf4j.Logger = Logger(this.getClass)

  override implicit def defaultAwaitTimeout: Timeout = 1.seconds
}

