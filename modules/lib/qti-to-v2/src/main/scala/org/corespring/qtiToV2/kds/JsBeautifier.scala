package org.corespring.qtiToV2.kds

import java.io.{IOException, InputStreamReader}

import org.mozilla.javascript._
import play.api.Play

/**
 * Provides a beautify method for pretty-printing Javascript. Lifted from
 * http://srccod.blogspot.com/2013/01/java-javascript-formatterbeautifier.html
 */
trait JsBeautifier {

  // TODO: This can't access the file.
  def beautify(js: String, indent: Int = 2): String = {
    val cx = Context.enter()
    val scope = cx.initStandardObjects()
    try {
      val reader = new InputStreamReader(getClass.getResourceAsStream("lib/beautify.js"))
      cx.evaluateReader(scope, reader, "__beautify.js", 1, null)
      reader.close()
    } catch {
      case e: IOException => throw new Error("Error reading beautify.js")
    }
    scope.put("js", scope, js)
    cx.evaluateString(scope, s"js_beautify(js, {indentSize: ${indent.toString}})", "inline", 1, null)
      .asInstanceOf[String]
  }

}