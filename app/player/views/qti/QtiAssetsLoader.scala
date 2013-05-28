package player.views.qti

trait QtiAssetsLoader {
  def localJsPaths: Seq[String]

  def remoteJsPaths: Seq[String]

  def localCssPaths: Seq[String]
}


