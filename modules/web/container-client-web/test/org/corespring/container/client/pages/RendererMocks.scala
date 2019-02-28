package org.corespring.container.client.pages

import org.corespring.container.client.component.ComponentJson
import org.corespring.container.client.controllers.apps.{ CssSourcePaths, NgSourcePaths, PageSourceService }
import org.corespring.container.client.controllers.helpers.PlayerXhtml
import org.corespring.container.client.pages.engine.JadeEngine
import org.corespring.container.client.pages.processing.AssetPathProcessor
import org.corespring.container.components.model.ComponentInfo
import org.corespring.container.components.processing.PlayerItemPreProcessor
import org.corespring.container.components.services.ComponentService
import org.specs2.mock.Mockito
import play.api.libs.json.{ JsValue, Json }
import play.api.templates.Html

/**
 * Some mocks with minimal defaults
 */
private[pages] object RendererMocks extends Mockito {

  def itemPreProcessor = {
    val m = mock[PlayerItemPreProcessor]
    m.preProcessItemForPlayer(any[JsValue]) answers { (s) => s.asInstanceOf[JsValue] }
    m
  }

  def jadeEngine = {
    val m = mock[JadeEngine]
    m.renderJade(any[String], any[Map[String, Any]]) returns Html("<html></html>")
    m
  }

  def pageSourceService = {
    val m = mock[PageSourceService]
    m.loadJs(any[String]) returns NgSourcePaths(Nil, "dest.js", Nil, Nil)
    m.loadCss(any[String]) returns CssSourcePaths(Nil, "dest.css", Nil)
    m
  }

  def playerXhtml = {
    val m = mock[PlayerXhtml]
    m.processXhtml(any[String]) answers { (s: Any) => s.asInstanceOf[String] }
    m
  }

  def componentJson = {
    val m = mock[ComponentJson]
    m.toJson(any[ComponentInfo]) returns Json.obj()
    m
  }

  def assetPathProcessor = {
    val m = mock[AssetPathProcessor]
    m.process(any[String]) answers { (s: Any) => s.asInstanceOf[String] }
    m
  }

  def componentService = {
    val m = mock[ComponentService]
    m.components returns Nil
    m.interactions returns Nil
    m.widgets returns Nil
    m.layoutComponents returns Nil
    m.libraries returns Nil
    m
  }
}
