package org.corespring.web.common.controllers

object DefaultCss {

  private def css(url: String): String = """<link rel="stylesheet" type="text/css" href="%s"/>""".format(url)

  private val BYTE_BUREAU = css("/assets/stylesheets/bytebureau/styles.css")

  val DEFAULT_CSS = Seq(BYTE_BUREAU).mkString("\n")
  val BOOTSTRAP = css(Bootstrap.path("css", "2.0.3"))
  val UBUNTU = css("//fonts.googleapis.com/css?family=Ubuntu:400,700,700italic,400italic")

}

object Bootstrap {
  def path(suffix: String, version: String = "2.1.1") = "/assets/bootstrap/%s/%s/bootstrap.%s".format(version, suffix, suffix)

}
