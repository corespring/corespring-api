package org.corespring.container.client.controllers.launcher

import org.corespring.container.client.io.ResourcePath
import org.corespring.container.client.views.txt.js.ServerLibraryWrapper
import play.api.libs.json.{ JsObject, Json }
import play.api.templates.TxtFormat

private[corespring] class JsBuilder(val resourcePath: ResourcePath) extends JsResource {

  override def load: (String) => Option[String] = resourcePath.loadPath(_)

  private def lib(n: String) = s"container-client/js/player-launcher/$n"

  lazy val coreJs: String = {

    val corePaths = {
      Some("container-client/bower_components/msgr.js/dist/msgr.js") ++
        Seq("logger", "callback-utils", "error-codes", "instance", "client-launcher", "url-builder", "object-id", "draft-id")
        .map(s => s"$s.js")
        .map(lib)
    }

    val rawJs = pathToNameAndContents("container-client/js/corespring/core-library.js")._2
    val wrapped = corePaths.map(pathToNameAndContents).map(t => ServerLibraryWrapper(t._1, t._2))
    val bootstrap =
      s"""
         |window.org = window.org || {};
         |org.corespring = org.corespring || {};
         |org.corespring.players = org.corespring.players || {};
         |org.corespring.editors = org.corespring.editors || {};
      """.stripMargin
    s"""$bootstrap
        $rawJs
        ${wrapped.mkString("\n")}
      """
  }

  def buildJs(corespringUrl: String, files: Seq[String], options: JsObject, bootstrapLine: String, queryParams: Map[String, String]): String = {

    val additionalJsNameAndSrc = files.map(lib(_)).map(pathToNameAndContents)

    val fullConfig = Json.obj(
      "corespringUrl" -> corespringUrl,
      "queryParams" -> queryParams) ++ options
    val fullConfigJs = ("launch-config" -> s"module.exports = ${Json.stringify(fullConfig)}")
    val wrappedNameAndContents = fullConfigJs +: additionalJsNameAndSrc
    val wrappedContents: Seq[TxtFormat.Appendable] = wrappedNameAndContents.map {
      case (name, content) => ServerLibraryWrapper(name, content)
    }

    s"""
       $coreJs
       ${wrappedContents.mkString("\n")}
       $bootstrapLine"""
  }

}