package org.corespring.qtiToV2

import scala.io.Source
import scala.xml._

object ManifestReader {

  val filename = "imsmanifest.xml"

  def read(manifest: Source): QTIManifest = {
    val resources = (XML.loadString(manifest.getLines.mkString).head \ "resources" \\ "resource")
      .partition(r => (r \ "@type").text.toString == "imsqti_item_xmlv2p1")

    QTIManifest(items =
      resources._1.map(n => {
        val filename = (n \ "@href").text.toString
        ManifestItem(id = (n \ "@identifier").text.toString, filename = filename, resources = (n \ "file").map(f => (f \ "@href").text.toString).filterNot(_ == filename))
      }),
      otherFiles = resources._2.map(n => (n \ "@href").text.toString))
  }

}

case class QTIManifest(
  items: Seq[ManifestItem] = Seq.empty,
  otherFiles: Seq[String] = Seq.empty)

case class ManifestItem(id: String, filename: String, resources: Seq[String] = Seq.empty)