package tests.filters

import filters.AjaxFilter

class AjaxFilterTest extends FilterTest(AjaxFilter) {

  "AjaxFilter" should {

    "add Cache-Control header when X-Requested-With is XMLHttpRequest" in {
      givenRequestHeader ("X-Requested-With", "XMLHttpRequest") shouldHaveResponseHeader ("Cache-Control", "no-cache")
    }

    "not add Cache-Control header when X-Requested-With is not XMLHttpRequest" in {
      givenRequestHeader ("X-Requested-With", "HttpRequest") shouldNotHaveResponseHeader "Cache-Control"
    }

    "not add Cache-Control header with X-Requested-With header missing" in {
      givenNoRequestHeaders shouldNotHaveResponseHeader "CacheControl"
    }

  }

}