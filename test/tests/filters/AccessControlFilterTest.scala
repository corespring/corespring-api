package tests.filters

import filters.AccessControlFilter

class AccessControlFilterTest extends FilterTest(AccessControlFilter) {

  "AccessControlFilter" should {

    "allow every domain" in {
      givenNoRequestHeaders shouldHaveResponseHeaders(
        ("Access-Control-Allow-Origin", "*"),
        ("Access-Control-Allow-Methods", "PUT, GET, POST, DELETE, OPTIONS"),
        ("Access-Control-Allow-Headers", "x-requested-with,Content-Type,Authorization")
      )
    }

  }

}
