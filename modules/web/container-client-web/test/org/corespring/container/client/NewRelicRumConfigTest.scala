package org.corespring.container.client

import com.typesafe.config.ConfigFactory
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.Configuration

class NewRelicRumConfigTest extends Specification {

  def configString(enabled: Boolean) =
    s"""{enabled:$enabled, license-key: key, application-id:id, agent: agent, script-path: "script"}"""

  class scope(configJson: String) extends Scope {

    val config = ConfigFactory.parseString(configJson)
    val configuration = new Configuration(config)
    val newRelicRumConfig = NewRelicRumConfig.fromConfig(configuration)
  }

  "apply" should {
    "return a new config" in new scope(configString(true)) {
      newRelicRumConfig must_== Some(NewRelicRumConfig("key", "id", "agent", "script"))
    }

    "not return a config if enabled is false" in new scope(configString(false)) {
      newRelicRumConfig === None
    }

    "be converted to proper json config object" in new scope(configString(true)) {
      newRelicRumConfig.map(_.json) === Some(NewRelicRumConfig("key", "id", "agent", "script").json)
    }
  }
}
