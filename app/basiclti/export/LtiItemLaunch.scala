package basiclti.export

import xml.Elem

case class LtiItemLaunch(itemId: String, title: String, description: Option[String] = None){
  def toXml:Elem = {
    var outer:Elem = <cartridge_basiclti_link xmlns="http://www.imsglobal.org/xsd/imslticc_v1p0"
                             xmlns:blti = "http://www.imsglobal.org/xsd/imsbasiclti_v1p0"
                             xmlns:lticm ="http://www.imsglobal.org/xsd/imslticm_v1p0"
                             xmlns:lticp ="http://www.imsglobal.org/xsd/imslticp_v1p0"
                             xmlns:xsi = "http://www.w3.org/2001/XMLSchema-instance">
      <blti:title>{title}</blti:title>
      <blti:custom>
        <lticm:property name="corespring_item_id">{itemId}</lticm:property>
      </blti:custom>
      <blti:launch_url>http://localhost:9000/basiclti</blti:launch_url>
    </cartridge_basiclti_link>
    if (description.isDefined){
      outer = new Elem(outer.prefix,outer.label,outer.attributes, outer.scope,
        (outer.child ++ <blti:description>{description.get}</blti:description>) : _*)
    }
    outer
  }
}
