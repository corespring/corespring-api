package org.corespring.qtiToV2.kds

import org.apache.commons.lang3.StringEscapeUtils
import org.corespring.qtiToV2.SourceWrapper

import scala.xml._

object ManifestReader extends ManifestFilter {

  val filename = "imsmanifest.xml"

  private def stripCDataTags(xmlString: String) =
    StringEscapeUtils.unescapeHtml4("""(?s)<!\[CDATA\[(.*?)\]\]>""".r.replaceAllIn(xmlString, "$1"))

  def read(manifest: SourceWrapper, sources: Map[String, SourceWrapper]): QTIManifest = {
    implicit val xml = filterManifest(manifest)
    val (qtiResources, resources) = (xml \ "resources" \\ "resource")
      .partition(r => (r \ "@type").text.toString == "imsqti_item_xmlv2p1")

    /**
     * This is rather dense. Essentially, this looks at all of the passage resources and scans their XML for references
     * to HTML video elements. A map is returned which maps the QTI resource filenames to video elements contained
     * within their corresponding passage XML.
     */
    val videos: Map[String, Seq[ManifestResource]] = resources.map(n => {
      (n \\ "file" \ "@href").map(_.text.toString)
        .filter(ManifestResourceType.fromPath(_) == ManifestResourceType.Passage)
        .map(p => sources.find{ case(path, source) => path == p }.map(_._2)
        .map(s => {
        val sourceFile = qtiResources.find(r => (r \\ "file").map(f => (f \ "@href").text.toString).contains(p))
          .map(r => (r \ "@href").text.toString).getOrElse(throw new Exception("Could not find source file"))
        sourceFile -> (XML.loadString(stripCDataTags(s.getLines.mkString))).map(xml => xml \\ "video" \ "source" \\ "@src")
          .map(_.text.toString).map(path => ManifestResource(path = """\.\/(.*)""".r.replaceAllIn(path, "$1"), resourceType = ManifestResourceType.Video))
      })
        )
    }.flatten).flatten.toMap

    val resourceLocators: Map[ManifestResourceType.Value, Node => Seq[String]] =
      Map(ManifestResourceType.Image -> (n => (n \\ "img").map(_ \ "@src").map(_.toString)))

    QTIManifest(items =
      qtiResources.map(n => {
        val filename = (n \ "@href").text.toString
        val files = sources.get(filename).map(file => XML.loadString(stripCDataTags(file.mkString))).map(node => {
          resourceLocators.map{ case (resourceType, fn) => resourceType -> fn(node) }.toMap
        }).getOrElse(Map.empty[ManifestResourceType.Value, Seq[String]]).map{ case(resourceType, filenames) => {
          filenames.map(filename => ManifestResource(path = """\.\/(.*)""".r.replaceAllIn(filename, "$1"), resourceType = resourceType))
        }}.flatten.toSeq

        ManifestItem(id = (n \ "@identifier").text.toString, filename = filename, resources = ((n \\ "file")
          .filterNot(f => (f \ "@href").text.toString == filename).map(f => {
          val path = (f \ "@href").text.toString
          ManifestResource(
            path = path,
            resourceType = ManifestResourceType.fromPath(path))
        })) ++ videos.get(filename).getOrElse(Seq.empty) ++ files)
      }),
      otherFiles = resources.map(n => (n \ "@href").text.toString))
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
  val QTI, Passage, Video, Image, Unknown = Value

  private val typeMap = Map(
    "imsqti_item_xmlv2p1" -> QTI,
    "passage" -> Passage
  )

  private val extensionMap = Map(
    Seq("gif", "jpeg", "jpg", "png") -> Image
  )

  private def fromExtension(path: String) = {
    def getExtension(path: String) = path.split("\\.").lastOption.getOrElse("")
    extensionMap.find{ case(extensions, resourceType) => extensions.contains(getExtension(path)) }
      .map(_._2).getOrElse(Unknown)
  }

  def fromPath(path: String)(implicit xml: Node): ManifestResourceType.Value =
    (xml \ "resources" \\ "resource").find(resource => (resource \ "@href").text.toString == path)
      .map(resource => (resource \ "@type").text.toString).map(typeMap.get(_)).flatten.getOrElse(fromExtension(path))

}