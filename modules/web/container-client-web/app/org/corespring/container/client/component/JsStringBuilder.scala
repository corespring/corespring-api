package org.corespring.container.client.component

trait JsStringBuilder {
  def buildJsString(e: (String, String)*): String = {
    val sb = new StringBuilder()
    e.foldLeft(sb)((acc, v) => {
      val (n, src) = v
      acc.append(s"// - $n\n$src\n// -")
    })
    sb.toString
  }
}

