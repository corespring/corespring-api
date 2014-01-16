package org.corespring.it

import org.specs2.specification.{Step, Fragments}
import play.api.mvc.Results
import play.api.test.PlaySpecification

class ITSpec extends PlaySpecification with Results with ServerSpec {

  sequential

  override def map(fs: => Fragments) = Step(server.start()) ^ fs ^ Step(server.stop)
}