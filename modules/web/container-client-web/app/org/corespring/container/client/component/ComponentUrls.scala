package org.corespring.container.client.component

import org.corespring.container.components.model.Component
import play.api.mvc.Controller

trait ComponentUrls {
  def jsUrl(context: String, components: Seq[Component], separatePaths: Boolean): Seq[String]
  def lessUrl(context: String, components: Seq[Component], separatePaths: Boolean, encodedCustomColors: Option[String]): Seq[String]
}

