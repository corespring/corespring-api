package org.corespring.container.client.component

import scala.concurrent.ExecutionContext

//case class to enable auto wiring
case class ComponentSetExecutionContext(heavyLoad: ExecutionContext)
