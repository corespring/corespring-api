package org.corespring.container.client.controllers.apps

import play.api.libs.json.JsValue

trait SourcePaths {
  def src: Seq[String]

  def dest: String

  def otherLibs: Seq[String]
}

case class CssSourcePaths(src: Seq[String], dest: String, otherLibs: Seq[String]) extends SourcePaths

case class NgSourcePaths(src: Seq[String], dest: String, otherLibs: Seq[String], ngModules: Seq[String]) extends SourcePaths

private[apps] case class Core(src: Seq[String], dest: String, otherLibs: Seq[String])

object SourcePaths {

  def prefixModule(prefix: String)(p: String) = if (p.startsWith("//")) p else s"$prefix$p"

  def withCore[SP <: SourcePaths](prefix: String, json: JsValue)(fn: Core => SP): SP = {
    val src = (json \ "src").as[Seq[String]].map(prefixModule(prefix))
    val dest = s"$prefix${(json \ "dest").as[String]}"
    val otherLibs = (json \ "libs").as[Seq[String]].map(prefixModule(prefix))
    fn(Core(src, dest, otherLibs))
  }

  def js(prefix: String, json: JsValue): NgSourcePaths = withCore[NgSourcePaths](prefix, json) { (core) =>
    val ngModules = (json \ "ngModules").asOpt[Seq[String]].getOrElse(Nil)
    NgSourcePaths(core.src, core.dest, core.otherLibs, ngModules)
  }

  def css(prefix: String, json: JsValue): CssSourcePaths = withCore[CssSourcePaths](prefix, json) { (core) =>
    CssSourcePaths(core.src, core.dest, core.otherLibs)
  }
}
