package org.corespring.container.client

import org.corespring.container.client.integration.ContainerExecutionContext

trait HasContainerContext {

  def containerContext: ContainerExecutionContext
  implicit val executionContext = containerContext.context

}
