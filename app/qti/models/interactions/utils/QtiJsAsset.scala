package qti.models.interactions.utils

case class QtiJsAsset(name:String, hasJsFile:Boolean = true, localDependents: Seq[String] = Seq(), remoteDependents: Seq[String] = Seq())


