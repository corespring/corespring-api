package developer.listeners

import org.corespring.it.IntegrationSpecification

/**
 * Note: this is a copy of the assertions in the old
 * <tests.plugins.UserEventListenerTest>
 * This old test was an integration test so it's been moved here.
 * TODO: Implement the assertion body.
 */
class UserEventListenerTest extends IntegrationSpecification {

  "UserEventListener" should {

    "update lastLoginDate on LoginEvent" in pending

    "update registrationDate on SignUpEvent" in pending

  }

}
