package org.corespring.it

import org.slf4j.LoggerFactory
import org.specs2.specification.{Step, Fragments}
import play.api.mvc.Results
import play.api.test.PlaySpecification

class IntegrationSpecification extends PlaySpecification with Results with ServerSpec {

  sequential

  protected def logger : org.slf4j.Logger = LoggerFactory.getLogger("it.spec")

  override def map(fs: => Fragments) = Step(server.start()) ^ fs ^ Step(server.stop)
}