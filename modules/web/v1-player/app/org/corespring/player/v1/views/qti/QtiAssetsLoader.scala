package org.corespring.player.v1.views.qti

trait QtiAssetsLoader {
  def localJsPaths: Seq[String]

  def remoteJsPaths: Seq[String]

  def localCssPaths: Seq[String]
}

