package org.corespring.v2.auth

import play.api.test.{PlaySpecification, FakeApplication}

trait TransformerSpec extends PlaySpecification{

  /**
   * TODO: Launching a fake app for a model spec shouldn't be necessary.
   * But we'd need to clean up the core module before we can do that.
   */
  val fakeApp = FakeApplication(
    additionalPlugins = Seq("se.radley.plugin.salat.SalatPlugin")
  )
}
