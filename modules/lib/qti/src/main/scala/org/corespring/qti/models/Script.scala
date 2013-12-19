package org.corespring.qti.models

import scala.xml.Node
import play.api.libs.json._
import com.scalapeno.rhinos.JavascriptProcessor

/**
 * Represents a script tag:
 *
 * <script type="text/javascript">
 *     console.log("Hello World!");
 * </script>
 */
case class Script(typeName: String, scriptBody: String) extends JavascriptProcessor {

  if (typeName != "text/javascript") throw new IllegalArgumentException("Cannot execute script of unknown type")

  def execute(): Option[Any] = execute(scriptBody, Map.empty)

  /**
   * execute lets you pass a Map[String, Any] of variables to be made available in the execution context of the
   * Javascript.
   */
  def execute(variables: Map[String, Any]): Option[(String,JsValue)] = execute(scriptBody, variables)

  private def execute(script: String, variables: Map[String, Any]): Option[(String,JsValue)] = {
    js(script, variables) match {
      case Some((script:String,jsValue: JsValue)) => Some(script ->jsValue)
      case _ => None
    }
  }

}

object Script {

  def apply(node: Node): Script = Script(typeName = (node \ "@type").text, scriptBody = node.child.text)
}
