package org.corespring.v2.player

import java.io.File

import com.typesafe.config.ConfigFactory
import org.apache.commons.io.{ FileUtils, IOUtils }
import org.corespring.container.client.CompressedAndMinifiedComponentSets
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.container.components.loader.ComponentLoader
import org.corespring.container.components.model.Component
import org.corespring.container.components.model.dependencies.DependencyResolver
import play.api.Mode.Mode
import play.api.{ Mode, Play, Configuration }

class CompressedComponentSets(
  val rootConfig: Configuration,
  val componentLoader: ComponentLoader,
  val mode: Mode,
  implicit val containerContext: ContainerExecutionContext) extends CompressedAndMinifiedComponentSets {

  import play.api.Play.current

  override def configuration: Configuration = {

    val c = ConfigFactory.parseString(
      s"""
         |minify: ${rootConfig.getBoolean("components.minify").getOrElse(mode == Mode.Prod)}
         |gzip: ${rootConfig.getBoolean("components.gzip").getOrElse(mode == Mode.Prod)}
           """.stripMargin)

    new Configuration(c)
  }

  override def loadLibrarySource(path: String): Option[String] = {
    val componentsPath = rootConfig.getString("components.path").getOrElse("?")
    val fullPath = s"$componentsPath/$path"
    val file = new File(fullPath)

    if (file.exists()) {
      logger.trace(s"load file: $path")
      Some(FileUtils.readFileToString(file, "UTF-8"))
    } else {
      Some(s"console.warn('failed to log $fullPath');")
    }
  }

  override def allComponents: Seq[Component] = componentLoader.all

  override def dependencyResolver: DependencyResolver = new DependencyResolver {
    override def components: Seq[Component] = allComponents
  }

  override def resource(path: String): Option[String] = Play.resource(s"container-client/bower_components/$path").map { url =>
    logger.trace(s"load resource $path")
    val input = url.openStream()
    val content = IOUtils.toString(input, "UTF-8")
    IOUtils.closeQuietly(input)
    content
  }

}
