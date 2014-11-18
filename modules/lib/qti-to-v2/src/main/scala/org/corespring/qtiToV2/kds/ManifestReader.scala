package org.corespring.qtiToV2.kds

import org.apache.commons.lang3.StringEscapeUtils
import org.corespring.qtiToV2.SourceWrapper
import org.corespring.qtiToV2.kds.interactions.PassageScrubber

import scala.xml._

object ManifestReader extends ManifestFilter with PassageScrubber {

  val filename = "imsmanifest.xml"

  private def stripCDataTags(xmlString: String) =
    StringEscapeUtils.unescapeHtml4("""(?s)<!\[CDATA\[(.*?)\]\]>""".r.replaceAllIn(xmlString, "$1")).replaceAll("&nbsp;", "")

  def read(manifest: SourceWrapper, sources: Map[String, SourceWrapper]): QTIManifest = {
    implicit val xml = filterManifest(manifest)
    val (qtiResources, resources) = (xml \ "resources" \\ "resource")
      .partition(r => (r \ "@type").text.toString == "imsqti_item_xmlv2p1")


    val resourceLocators: Map[ManifestResourceType.Value, Node => Seq[String]] =
      Map(
        ManifestResourceType.Image -> (n => (n \\ "img").map(_ \ "@src").map(_.toString)),
        ManifestResourceType.Video -> (n => (n \\ "video" \ "source" \\ "@src").map(_.text.toString))
      )

    QTIManifest(items =
      qtiResources.map(n => {
        val filename = (n \ "@href").text.toString
        val files = sources.get(filename).map { file =>
          try {
            Some(XML.loadString(stripCDataTags(file.mkString)))
          } catch {
            case e: Exception => {
              println(s"Error reading: $filename")
              None
            }
          }
        }.flatten.map(node => {
          resourceLocators.map{ case (resourceType, fn) => resourceType -> fn(node) }.toMap
        }).getOrElse(Map.empty[ManifestResourceType.Value, Seq[String]]).map{ case(resourceType, filenames) => {
          filenames.map(filename => ManifestResource(path = """\.\/(.*)""".r.replaceAllIn(filename, "$1"), resourceType = resourceType))
        }}.flatten.toSeq

        val resources = ((n \\ "file")
          .filterNot(f => (f \ "@href").text.toString == filename).map(f => {
          val path = (f \ "@href").text.toString
          ManifestResource(
            path = path,
            resourceType = ManifestResourceType.fromPath(path))
        })) ++ files

        val passageResources: Seq[ManifestResource] = resources.filter(_.is(ManifestResourceType.Passage)).map(p =>
          sources.find { case (path, _) => path == p.path}.map{case (filename, s) => {
            try {
              Some((XML.loadString(scrub(stripCDataTags(s.getLines.mkString)))).map(xml => resourceLocators.map {
                case(resourceType, fn) => (resourceType, fn(xml))}).flatten.map { case (resourceType, paths) =>
                paths.map(path => ManifestResource(path = """\.\/(.*)""".r.replaceAllIn(path, "$1"), resourceType = resourceType))
              }.flatten)
            } catch {
              case e: Exception => {
                println(s"Error reading: $filename")
                e.printStackTrace
                None
              }
            }
          }}.flatten
        ).flatten.flatten
        ManifestItem(id = (n \ "@identifier").text.toString, filename = filename, resources = resources ++ passageResources)
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

  private val extensionMap = Map(Seq("gif", "jpeg", "jpg", "png") -> Image)
  private val pathFunctions: Seq[String => Option[ManifestResourceType.Value]] = Seq(
    (path => path.startsWith("passages/") match {
      case true => Some(ManifestResourceType.Passage)
      case _ => None
    })
  )

  private def fromPathString(path: String): ManifestResourceType.Value = {
    def getExtension(path: String) = path.split("\\.").lastOption.getOrElse("")
    pathFunctions.map(_(path)).find(_.nonEmpty).flatten.getOrElse(
      extensionMap.find{ case(extensions, resourceType) => extensions.contains(getExtension(path)) }
        .map(_._2).getOrElse(Unknown))
  }

  def fromPath(path: String)(implicit xml: Node): ManifestResourceType.Value =
    (xml \ "resources" \\ "resource").find(resource => (resource \ "@href").text.toString == path)
      .map(resource => (resource \ "@type").text.toString).map(typeMap.get(_)).flatten.getOrElse(fromPathString(path))

}