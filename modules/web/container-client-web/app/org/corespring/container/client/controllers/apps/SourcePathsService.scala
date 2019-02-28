package org.corespring.container.client.controllers.apps

case class ContextAndSuffix(context: String, suffix: String)

trait PageSourceService {
  def loadJs(key: String): NgSourcePaths
  def loadCss(key: String): CssSourcePaths
}

import play.api.Logger
import play.api.libs.json.{ JsValue, Json }

import scala.collection.mutable

case class PageSourceServiceConfig(prefix: String, reload: Boolean, load: String => Option[String])

class JsonPageSourceService(config: PageSourceServiceConfig) extends PageSourceService {

  private val logger = Logger(classOf[JsonPageSourceService])

  private val reports: mutable.Map[ContextAndSuffix, SourcePaths] = mutable.Map.empty

  def reportName(contextAndSuffix: ContextAndSuffix): String = {
    val suffix = contextAndSuffix.suffix
    val context = contextAndSuffix.context
    val out = s"container-client/$context-$suffix-report.json"
    logger.debug(s"function=reportName, contextAndSuffix=$contextAndSuffix, out=$out")
    out
  }

  private def load[A <: SourcePaths](contextAndSuffix: ContextAndSuffix, load: (String) => A): A = {
    logger.info(s"function=load, contextAndSuffix=$contextAndSuffix")
    (reports.get(contextAndSuffix), config.reload) match {
      case (Some(sp), false) => sp.asInstanceOf[A]
      case _ => {
        val path = reportName(contextAndSuffix)
        logger.debug(s"function=load, path=$path - load report")
        val report = load(path)
        reports.put(contextAndSuffix, report)
        report
      }
    }
  }

  private def pathToJson(path: String): Option[JsValue] = {
    config.load(path).flatMap { jsonString =>
      try {
        Some(Json.parse(jsonString))
      } catch {
        case _: Throwable => None
      }
    }
  }

  override def loadJs(key: String): NgSourcePaths = {
    load[NgSourcePaths](ContextAndSuffix(key, "js"), path => {
      val json = pathToJson(path).getOrElse(throw new IllegalArgumentException(s"Can't load path $path"))
      logger.trace(s"function=loadJs, json=${Json.prettyPrint(json)}")
      SourcePaths.js(config.prefix, json)
    })
  }

  override def loadCss(key: String): CssSourcePaths = {
    load(ContextAndSuffix(key, "css"), path => {
      val json = pathToJson(path).getOrElse(throw new IllegalArgumentException(s"Can't load path $path"))
      logger.trace(s"function=loadCss, json=${Json.prettyPrint(json)}")
      SourcePaths.css(config.prefix, json)
    })
  }
}

