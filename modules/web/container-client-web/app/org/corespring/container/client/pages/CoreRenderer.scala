package org.corespring.container.client.pages

import org.corespring.container.client.component.Bundle
import org.corespring.container.client.controllers.apps.PageSourceService
import org.corespring.container.client.pages.processing.AssetPathProcessor

trait CoreRenderer {
  def pageSourceService: PageSourceService
  def assetPathProcessor: AssetPathProcessor
  def name: String
  lazy val sources = Sources(name, pageSourceService)

  def prepareJsCss(prodMode: Boolean, bundle: Bundle) = {
    val css = if (prodMode) Seq(sources.css.dest) else sources.css.src
    val js = if (prodMode) Seq(sources.js.dest) else sources.js.src
    val processedCss = (sources.css.otherLibs ++ css ++ bundle.css).map(assetPathProcessor.process)
    val processedJs = (sources.js.otherLibs ++ js ++ bundle.js).map(assetPathProcessor.process)
    (processedJs, processedCss)
  }

  def jsArrayString(s: Iterable[String]) = s.map { s => s"'$s'" }.mkString(",")
}
