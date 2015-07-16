package org.corespring.web.common.controllers

object DefaultCss {

  val BOOTSTRAP = css(bootstrap("css", "2.0.3"))

  val UBUNTU = css("//fonts.googleapis.com/css?family=Ubuntu:400,700,700italic,400italic")

  def bootstrap(suffix: String, version: String = "2.1.1") = s"/assets/bootstrap/$version/$suffix/bootstrap.$suffix"

  private def css(url: String): String = s""" <link rel="stylesheet" type="text/css" href="$url"></link> """

}

