package org.corespring.container.client.integration

import play.api.Configuration

trait HasConfig {
  def configuration: Configuration
}

