package org.corespring.container.client.controllers

import com.softwaremill.macwire.MacwireMacros.wire
import org.corespring.container.client.component.{ ComponentSetExecutionContext, ComponentsConfig }
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.container.client.io.ResourcePath
import org.corespring.container.components.services.{ ComponentService, DependencyResolver }

trait ComponentControllersModule {
  def componentSetExecutionContext: ComponentSetExecutionContext
  def containerContext: ContainerExecutionContext
  def resourceLoader: ResourcePath
  def componentService: ComponentService
  def dependencyResolver: DependencyResolver
  def componentsConfig: ComponentsConfig

  lazy val componentSets: ComponentSets = wire[CompressedAndMinifiedComponentSets]
  lazy val componentsFileController: ComponentsFileController = wire[ComponentsFileController]
  lazy val icons: Icons = wire[Icons]
  lazy val componentControllers = Seq(componentSets, componentsFileController, icons)
}
