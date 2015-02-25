package org.corespring.qtiToV2

trait HtmlProcessor extends EntityEscaper {

  def preprocessHtml(html: String) = {
    escapeEntities(Windows1262EntityTransformer.transform(html))
  }

  def postprocessHtml(html: String) = {

  }

}
