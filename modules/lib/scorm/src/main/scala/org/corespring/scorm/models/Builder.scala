package org.corespring.scorm.models

import org.corespring.platform.core.models.item.Item
import play.api.libs.json.{Json, JsObject, JsString}
import scala.xml.{ Unparsed, Elem }

object Builder {

  case class Config(corespringDomain: String)

  object Manifest {

    def apply(items: List[Item], config: Config): Elem = <manifest identifier="com.corespring.scormwrapper" version="1" xmlns="http://www.imsglobal.org/xsd/imscp_v1p1" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:adlcp="http://www.adlnet.org/xsd/adlcp_v1p3" xmlns:adlseq="http://www.adlnet.org/xsd/adlseq_v1p3" xmlns:adlnav="http://www.adlnet.org/xsd/adlnav_v1p3" xmlns:imsss="http://www.imsglobal.org/xsd/imsss" xsi:schemaLocation="http://www.imsglobal.org/xsd/imscp_v1p1 imscp_v1p1.xsd
                              http://www.adlnet.org/xsd/adlcp_v1p3 adlcp_v1p3.xsd
                              http://www.adlnet.org/xsd/adlseq_v1p3 adlseq_v1p3.xsd
                              http://www.adlnet.org/xsd/adlnav_v1p3 adlnav_v1p3.xsd
                              http://www.imsglobal.org/xsd/imsss imsss_v1p0.xsd">
                                                           <metadata>
                                                             <schema>ADL SCORM</schema>
                                                             <schemaversion>2004 4th Edition</schemaversion>
                                                           </metadata>
                                                           <organizations default="some_test_org">
                                                             <organization>
                                                               <title>My Corespring Items</title>
                                                               <item identifier="section_one">
                                                                 <title>Section One</title>{ for (i <- items) yield ItemNode(i, config) }
                                                               </item>
                                                             </organization>
                                                           </organizations>
                                                           <resources>
                                                             { for (i <- items) yield ResourceNode(i) }
                                                           </resources>
                                                         </manifest>

  }

  object ResourceNode {
    def apply(item: Item): Elem = {
      <resource identifier={ item.id.toString } type="webcontent" adlcp:scormType="sco" href="remote-item-runner.html"></resource>
    }
  }

  object ItemLaunchData {
    def apply(item: Item, config: Config): Unparsed = {
      val launchData = Seq(
        "itemId" -> JsString(item.id.toString),
        "templates" -> JsObject(Seq(
          "item" -> JsString(config.corespringDomain + "/scorm/player/item/:itemId/run"),
          "session" -> JsString(config.corespringDomain + "/scorm/player/session/:sessionId/run"))))

      val dataString = Json.prettyPrint(JsObject(launchData))
      Unparsed(dataString)
    }
  }

  object ItemNode {
    def apply(item: Item, config: Config): Elem = {

      <item identifier={ item.id.toString } identifierref={ item.id.toString }>
        <title>
          { item.taskInfo.map(_.title.getOrElse("?")) }
        </title>
        <adlcp:dataFromLMS>
          { ItemLaunchData(item, config) }
        </adlcp:dataFromLMS>
        <adlcp:data>
          <adlcp:map targetID="shared_data"/>
          <adlcp:map targetID={ item.id.toString }/>
        </adlcp:data>
      </item>

    }
  }

}

