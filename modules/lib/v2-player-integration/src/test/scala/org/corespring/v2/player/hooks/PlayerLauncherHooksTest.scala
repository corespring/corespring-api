package org.corespring.v2.player.hooks

import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.v2.auth.models.{ AuthMode, MockFactory, OrgAndOpts }
import org.corespring.v2.errors.V2Error
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.mvc.RequestHeader
import play.api.test.{ FakeRequest, PlaySpecification }

import scala.concurrent.ExecutionContext
import scalaz.{ Success, Validation }

class PlayerLauncherHooksTest extends Specification with MockFactory with PlaySpecification {

  trait scope extends Scope {
    def orgAndOpts = Success(mockOrgAndOpts(AuthMode.ClientIdAndPlayerToken))
    def getOrgAndOptsFn(rh: RequestHeader): Validation[V2Error, OrgAndOpts] = orgAndOpts
    val req = FakeRequest("", "")
    val hooks = new PlayerLauncherHooks(
      getOrgAndOptsFn,
      ContainerExecutionContext(ExecutionContext.global))
  }

  "componentEditor" should {

    /**
     * TODO:
     * testing with a [[play.api.mvc.Session]] in the mix means you have to boot a FakeApplication
     * Which is a bit cumbersome. Mocking the Session causes other play weirdness.
     */
    "return js with no warnings" in pending
  }
}
