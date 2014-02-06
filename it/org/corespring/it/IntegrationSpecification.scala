package org.corespring.it

import org.slf4j.LoggerFactory
import org.specs2.specification.{ Step, Fragments }
import play.api.mvc.Results
import play.api.test.PlaySpecification

class IntegrationSpecification extends PlaySpecification with Results with ServerSpec {

  sequential

  protected def logger: org.slf4j.Logger = LoggerFactory.getLogger("it.spec.is")

  override def map(fs: => Fragments) = {
    Step(server.start()) ^
      Step(logger.trace("-------------------> server started")) ^
      fs ^
      Step(logger.trace("-------------------> stopping server")) ^
      Step(server.stop)
  }
}