package org.corespring.container.client.component

import org.corespring.container.client.controllers.helpers.{ LoadClientSideDependencies, NameHelper }
import org.corespring.container.client.views.txt.js.{ ComponentServerWrapper, ComponentWrapper, ServerLibraryWrapper }
import org.corespring.container.components.model._
import org.corespring.container.components.model.packaging.ClientSideDependency
import org.corespring.container.components.services.ComponentTypeFilter
import play.api.libs.json.{ JsObject, JsValue, Json }
import org.apache.commons.io.IOUtils
import org.corespring.container.client.controllers.apps.StaticPaths

object SourceGenerator {
  object Keys {
    val LocalLibs = "local libs"
    val ThirdParty = "3rd party"
    val Libraries = "libraries"
    val Interactions = "Interactions"
    val Widgets = "Widgets"
    val Layout = "Layout"
  }
}

trait SourceGenerator
  extends ComponentTypeFilter
  with NameHelper {

  def assetPath: String

  def js(components: Seq[Component]): String

  def less(components: Seq[Component], customColors: JsObject = Json.obj()): String

  def layoutToJs(layout: LayoutComponent): String = {
    layout.client.map(wrapClientLibraryJs(moduleName(layout.org, layout.name))).mkString("\n")
  }

  protected def wrapComponent(moduleName: String, directiveName: String, src: String) = {
    ComponentWrapper(moduleName, directiveName, src, assetPath).toString
  }

  protected def libraryToJs(l: Library): String

  protected def addLibraryJs(addClient: Boolean, addServer: Boolean)(l: Library): String = {

    def wrapServerLibraryJs(src: LibrarySource) = {
      s"""
      // ----------------- ${src.name} ---------------------
      ${ServerLibraryWrapper(src.name, src.source)}
      """
    }

    val libs = if (addClient) l.client.map(wrapClientLibraryJs(moduleName(l.org, l.name))).mkString("\n") else ""
    val server = if (addServer) l.server.map(wrapServerLibraryJs).mkString("\n") else ""

    s"""
    // -------------------- Libraries -----------------------
    $libs
    $server
    """
  }

  protected def wrapClientLibraryJs(moduleName: String)(src: LibrarySource) = {
    s"""
      // ----------------- ${src.name} ---------------------
      ${ComponentWrapper(moduleName, src.name, src.source, assetPath)}
      """
  }

  protected def splitComponents(comps: Seq[Component]): (Seq[Library], Seq[Interaction], Seq[LayoutComponent], Seq[Widget]) = (
    filterByType[Library](comps),
    filterByType[Interaction](comps),
    filterByType[LayoutComponent](comps),
    filterByType[Widget](comps))
}

trait ResourceLoading {
  def resource(path: String): Option[String]
}

trait LibrarySourceLoading {
  def loadLibrarySource(path: String): Option[String]
}

abstract class BaseGenerator
  extends SourceGenerator
  with LoadClientSideDependencies
  with ResourceLoading
  with LibrarySourceLoading
  with JsStringBuilder {

  protected def get3rdPartyScripts(dependencies: Seq[ClientSideDependency]): Seq[String] = {
    val paths: Seq[String] = dependencies.map(d => {
      val dir = d.dirOverride.getOrElse(d.name)
      d.jsFiles.map { name => s"${dir}/$name" }
    }).flatten.distinct
    paths.flatMap(resource)
  }

  protected def getStyles(dependencies: Seq[ClientSideDependency]): Seq[String] = {
    val paths: Seq[String] = dependencies.map(d => d.cssFiles.map { name => s"${d.name}/$name" }).flatten.distinct
    paths.flatMap(resource)
  }

  protected def getLibScripts(components: Seq[Component]): Seq[String] = {

    val allPaths: Seq[String] = {
      for {
        comp <- components
        lib <- (comp.packageInfo \ "libs").asOpt[JsObject]
        client <- (lib \ "client").asOpt[JsObject]
        paths <- Some(client.fields.map(_._2.as[Seq[String]]).flatten)
      } yield {
        for (p <- paths) yield s"${comp.id.org}/${comp.id.name}/libs/$p"
      }
    }.flatten.distinct

    allPaths.flatMap(loadLibrarySource)
  }

  override def less(components: Seq[Component], customColors: JsObject = Json.obj()): String = {
    val inputStream = this.getClass().getResourceAsStream("/public/components-common.less")
    val commonLessSource = IOUtils.toString(inputStream)
    inputStream.close
    val commonLess = commonLessSource.mkString
    val (libraries, uiComps, layoutComps, widgets) = splitComponents(components)
    val uiLess = uiComps.map(_.client.less.getOrElse("")).mkString("\n")
    val widgetLess = widgets.map(_.client.less.getOrElse("")).mkString("\n")
    val layoutLess = layoutComps.map(_.less.getOrElse("")).mkString("\n")
    val libraryLess = libraries.map(_.less.getOrElse("")).mkString("\n")
    val dependencies = getClientSideDependencies(components)
    val styles = getStyles(dependencies).mkString("\n")
    val dynamicColors = customColors.asOpt[Map[String, String]] match {
      case Some(cols) => cols.map { case (a, b) => s"@$a: $b;" }.mkString("\n")
      case _ => ""
    }
    s"""
       |$commonLess
       |$dynamicColors
       |$uiLess
       |$widgetLess
       |$layoutLess
       |$libraryLess
       |$styles
        """.stripMargin
  }

  override def js(components: Seq[Component]): String = {
    val (libs, uiComps, layoutComps, widgets) = splitComponents(components)
    val uiJs = uiComps.map(interactionToJs).mkString("\n")
    val widgetJs = widgets.map(widgetToJs).mkString("\n")
    val libJs = libs.map(libraryToJs).mkString("\n")
    val layoutJs = layoutComps.map(layoutToJs).mkString("\n")

    val dependencies = getClientSideDependencies(components)
    val scripts = get3rdPartyScripts(dependencies).mkString("\n")
    val libScripts = getLibScripts(components).mkString("\n")

    import SourceGenerator.Keys._

    buildJsString(
      LocalLibs -> libScripts,
      ThirdParty -> scripts,
      Libraries -> libJs,
      Interactions -> uiJs,
      Widgets -> widgetJs,
      Layout -> layoutJs)
  }

  protected def header(id: Id, msg: String) = s"""
      // -----------------------------------------
      // ${id.org} ${id.name} | $msg
      // -----------------------------------------
  """

  protected def wrapWithHeader(id: Id, js: String, msg: String) = {
    val m = moduleName(id.org, id.name)
    val d = directiveName(id.org, id.name)
    s"""
       | ${header(id, msg)}
       | ${wrapComponent(m, d, js)}
     """.stripMargin
  }

  private def previewJs(id: Id, js: String) = wrapWithHeader(id, js, "Client Preview")

  def widgetToJs(ui: Widget): String = previewJs(ui.id, ui.client.render)

  def interactionToJs(ui: Interaction): String = previewJs(ui.id, ui.client.render) + "\n" +
    ui.client.renderLibs.map(wrapClientLibraryJs(moduleName(ui.id.org, ui.id.name))).mkString("\n")
}

trait BaseWithWrappingGenerator extends BaseGenerator {

  private def configJs(id: Id, js: String) = wrapJs(id, js, "Config", "Client Config")

  private def wrapJs(id: Id, js: String, suffix: String, msg: String) = {
    val m = moduleName(id.org, id.name)
    val d = s"${directiveName(id.org, id.name)}$suffix"

    s"""
       | ${header(id, msg)}
       | ${wrapComponent(m, d, js)}
     """.stripMargin
  }

  private def serverJs(componentType: String, definition: String): String = ComponentServerWrapper(componentType, definition).toString

  override def interactionToJs(i: Interaction): String = {
    val base = super.interactionToJs(i)
    val cfg = configJs(i.id, i.client.configure) + "\n" +
      i.client.configureLibs.map(wrapClientLibraryJs(moduleName(i.id.org, i.id.name))).mkString("\n")
    val server = serverJs(i.componentType, i.server.definition)
    s"""
       |$base
       |$cfg
       |$server
     """.stripMargin
  }

  override def widgetToJs(w: Widget): String = {
    val base = super.widgetToJs(w)
    val cfg = configJs(w.id, w.client.configure)
    s"""
       |$base
       |$cfg
     """.stripMargin
  }

  override protected def libraryToJs(l: Library): String = addLibraryJs(true, true)(l)
}

trait EditorGenerator extends BaseWithWrappingGenerator {

}

trait CatalogGenerator extends BaseWithWrappingGenerator {

}

trait PlayerGenerator extends BaseGenerator with JsStringBuilder {
  override protected def libraryToJs(l: Library): String = addLibraryJs(true, false)(l)
}

