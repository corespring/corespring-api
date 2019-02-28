package org.corespring.container.client.pages.engine

import java.io.{ Reader, StringReader }

import de.neuland.jade4j.exceptions.JadeLexerException
import de.neuland.jade4j.template.{ JadeTemplate, TemplateLoader }
import de.neuland.jade4j.{ Jade4J, JadeConfiguration }
import grizzled.slf4j.Logger
import org.apache.commons.io.IOUtils
import play.api.Mode
import play.api.Mode.Mode
import play.api.templates.Html

import scala.collection.mutable

case class JadeEngineConfig(root: String, mode: Mode, loadStringFromPath: String => Option[String], lastModifiedFromPath: String => Option[Long])

class JadeEngine(config: JadeEngineConfig) {

  private def mode = config.mode
  private def root = config.root

  private lazy val logger = Logger(classOf[JadeEngine])

  private val templates: mutable.Map[String, JadeTemplate] = mutable.Map()

  private val readers: mutable.Stack[Reader] = new mutable.Stack[Reader]()

  private class InternalTemplateLoader(val root: String) extends TemplateLoader {

    private def toPath(name: String) = {
      s"$root/$name${if (name.endsWith(".jade")) "" else ".jade"}"
    }

    override def getLastModified(name: String): Long = config.lastModifiedFromPath(toPath(name)).getOrElse {
      throw new RuntimeException(s"getLastModified - can't get lastModified from path: ${toPath(name)}")
    }

    override def getReader(name: String): Reader = {
      logger.trace(s"getReader name=$name")
      val expandedPath = toPath(name)
      config.loadStringFromPath(expandedPath).map { string =>
        val returnValue = new StringReader(string)
        readers.push(returnValue)
        returnValue
      }.getOrElse { throw new RuntimeException(s"getReader - Unable to load jade file as a resource from: ${expandedPath}") }
    }
  }

  val jadeConfig = {
    val c = new JadeConfiguration
    c.setTemplateLoader(new InternalTemplateLoader(root))
    c.setMode(Jade4J.Mode.HTML)
    c.setPrettyPrint(mode == Mode.Dev)
    c
  }

  private def cleanupReaders() = {
    readers.foreach { r =>
      IOUtils.closeQuietly(r)
    }
    readers.clear()
  }

  private def loadTemplate(name: String): JadeTemplate = {
    def readIn = try {
      val out = jadeConfig.getTemplate(name)
      templates.put(name, out)
      cleanupReaders()
      out
    } catch {
      case jle: JadeLexerException => {
        val line = jle.getTemplateLines.get(jle.getLineNumber)
        logger.error(s"jade error: ${jle.getFilename}, line no: ${jle.getLineNumber}\n> $line")
        throw jle
      }
      case t: Throwable => throw t
    }

    if (mode == Mode.Dev) {
      jadeConfig.clearCache()
      readIn
    } else templates.get(name).getOrElse { readIn }
  }

  def renderJade(name: String, params: Map[String, Any]): Html = {
    require(params != null, "params is null")
    import scala.collection.JavaConversions._
    val template = loadTemplate(name)
    logger.trace(s"function=renderJade template=$template")
    logger.trace(s"function=renderJade params=$params")
    val jadeParams = params.asInstanceOf[Map[String, AnyRef]]
    val rendered = jadeConfig.renderTemplate(template, jadeParams)
    Html(new StringBuilder(rendered).toString)
  }
}
