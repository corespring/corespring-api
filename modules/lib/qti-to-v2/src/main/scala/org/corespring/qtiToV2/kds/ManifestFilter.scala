package org.corespring.qtiToV2.kds

import org.corespring.qtiToV2.SourceWrapper

import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.xml.pull.{EvText, EvElemEnd, EvElemStart, XMLEventReader}
import scala.xml.{XML, NodeSeq, MetaData, Node}

trait ManifestFilter {

  val ItemTypeIds = Map(
    7 -> "Selectable Text",
    8 -> "Multiple Part (EBSR)",
    10 -> "Drag and Drop-Classification",
    11 -> "Evidence Based Selected Response",
    13 -> "Graphing-Number Line",
    15 -> "Graphing-Place points on a coordinate grid",
    16 -> "Graphing-Place lines on a coordinate grid",
    17 -> "Matching Tables"
  )

  val ImportableItemTypeIds = Seq(8,11,13,15,16,17)

  /**
   * KDS manifest files are ~200mb, and we're only interested in a few hundred results. This method uses Scala's XML
   * pull-parser to create a manifest that includes only the files we're really interested in.
   */
  def filterManifest(file: SourceWrapper): Node = {

    def getResources(source: Source, keepNode: MetaData => Boolean): Seq[NodeSeq] = {
      var resources: Seq[String] = Seq.empty
      val xml = new XMLEventReader(source)

      def writeBuffer(buffer: ArrayBuffer[String]) = resources = resources :+ buffer.mkString

      def parse(xml: XMLEventReader) = {
        var buf = ArrayBuffer[String]()
        var insideResource = false
        var insideItemTypeId = false
        for (event <- xml) {
          event match {
            case EvElemStart(_, "resource", attrs, _) => {
              if (keepNode(attrs)) {
                insideResource = true
                buf += s"<resource${attrs.toString}>"
              }
            }
            case EvElemStart(_, "itemTypeId", _, _) => {
              buf += "<itemTypeId>"
              insideItemTypeId = true
            }
            case EvElemEnd(_, "resource") => {
              val tag = "</resource>"
              buf += tag
              if (insideResource) {
                writeBuffer(buf)
              }
              insideResource = false
              buf.clear
            }
            case EvElemEnd(_, "itemTypeId") => {
              buf += "</itemTypeId>"
              insideItemTypeId = false
            }
            case e @ EvElemStart(_, tag, attrs, _) => {
              if (insideResource) {
                buf += (attrs.isEmpty match {
                  case true => s"<$tag>"
                  case _ => s"<$tag ${attrs.toString}>"
                })
              }
            }
            case e @ EvElemEnd(_, tag) => {
              if (insideResource) {
                buf += s"</$tag>"
              }
            }
            case EvText(text) => {
              if (insideItemTypeId) {
                if (ImportableItemTypeIds.map(_.toString).toSeq.contains(text)) {
                  buf += text
                } else {
                  insideResource = false
                  buf.clear
                }
              } else if (insideResource) {
                buf += text
              }
            }
          }
        }
      }

      parse(xml)
      resources.map(XML.loadString(_))
    }

    val ofType: (MetaData, String) => Boolean = { case(attr, tipe) => attr.get("type").getOrElse("").toString == tipe }

    val resources = getResources(file.toSource(), attrs => ofType(attrs, "imsqti_item_xmlv2p1"))
    val files = resources.map(_ \\ "file" \ "@href").map(_.toString).filter(_.nonEmpty)
    val passages = getResources(file.toSource(), attrs => ofType(attrs, "passage") && files.contains(attrs.get("href").getOrElse("").toString))

    <manifest xmlns="http://www.imsglobal.org/xsd/imscp_v1p1" xmlns:imsmd="http://www.imsglobal.org/xsd/imsmd_v1p2" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:imsqti="http://www.imsglobal.org/xsd/imsqti_v2p1" identifier="MANIFEST-10/3/2014" xsi:schemaLocation="http://www.imsglobal.org/xsd/imscp_v1p1 imscp_v1p1.xsd http://www.imsglobal.org/xsd/imsmd_v1p2 imsmd_v1p2p4.xsd http://www.imsglobal.org/xsd/imsqti_v2p1  http://www.imsglobal.org/xsd/imsqti_v2p1.xsd">
      <resources>
        {resources ++ passages}
      </resources>
    </manifest>
  }


}
