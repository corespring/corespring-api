package org.corespring.qtiToV2.kds

import org.apache.commons.lang3.StringEscapeUtils

import scala.io.Source
import scala.xml._

object ManifestReader {

  val filename = "imsmanifest.xml"

  private def stripCDataTags(xmlString: String) =
    StringEscapeUtils.unescapeHtml4("""(?s)<!\[CDATA\[(.*?)\]\]>""".r.replaceAllIn(xmlString, "$1"))

  def read(manifest: Source, sources: Map[String, Source]): QTIManifest = {
    implicit val xml = XML.loadString(manifest.getLines.mkString).head
    val resources = (xml \ "resources" \\ "resource")
      .partition(r => (r \ "@type").text.toString == "imsqti_item_xmlv2p1")

    /**
     * This is rather dense. Essentially, this looks at all of the passage resources and scans their XML for references
     * to HTML video elements. A map is returned which maps the QTI resource filenames to video elements contained
     * within their corresponding passage XML.
     */
    val videos: Map[String, Seq[ManifestResource]] = resources._2.map(n => {
      (n \\ "file" \ "@href").map(_.text.toString)
        .filter(ManifestResourceType.fromPath(_) == ManifestResourceType.Passage)
        .map(p => sources.find{ case(path, source) => path == p }.map(_._2)
          .map(s => {
            val sourceFile = resources._1.find(r => (r \\ "file").map(f => (f \ "@href").text.toString).contains(p))
              .map(r => (r \ "@href").text.toString).getOrElse(throw new Exception("Could not find source file"))
            sourceFile -> (XML.loadString(stripCDataTags(s.getLines.mkString))).map(xml => xml \\ "video" \ "source" \\ "@src")
              .map(_.text.toString).map(path => ManifestResource(path = path, resourceType = ManifestResourceType.Video))
          })
        )
    }.flatten).flatten.toMap

    QTIManifest(items =
      resources._1.map(n => {
        val filename = (n \ "@href").text.toString
        ManifestItem(id = (n \ "@identifier").text.toString, filename = filename, resources = ((n \\ "file")
          .filterNot(f => (f \ "@href").text.toString == filename).map(f => {
            val path = (f \ "@href").text.toString
            ManifestResource(
              path = path,
              resourceType = ManifestResourceType.fromPath(path))
        })) ++ videos.get(filename).getOrElse(Seq.empty))
      }),
      otherFiles = resources._2.map(n => (n \ "@href").text.toString))
  }
}

case class QTIManifest(
  items: Seq[ManifestItem] = Seq.empty,
  otherFiles: Seq[String] = Seq.empty)

case class ManifestItem(id: String, filename: String, resources: Seq[ManifestResource] = Seq.empty)

case class ManifestResource(path: String, resourceType: ManifestResourceType.Value) {
  def is(resourceType: ManifestResourceType.Value) = this.resourceType == resourceType
}

object ManifestResourceType extends Enumeration {
  type ManifestResourceType = Value
  val QTI, Passage, Video, Unknown = Value

  private val typeMap = Map(
    "imsqti_item_xmlv2p1" -> QTI,
    "passage" -> Passage,
    "video/mp4" -> Video
  )

  def fromPath(path: String)(implicit xml: Node): ManifestResourceType.Value =
    (xml \ "resources" \\ "resource").find(resource => (resource \ "@href").text.toString == path)
      .map(resource => (resource \ "@type").text.toString).map(typeMap.get(_)).flatten.getOrElse(Unknown)

}