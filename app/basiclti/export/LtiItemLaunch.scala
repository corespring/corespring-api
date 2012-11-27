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

case class LtiItemOutcomes(itemId: String, title: String, description: Option[String] = None){
  def toXml:Elem = {
    var outer = <cartridge_basiclti_link xmlns:lticm="http://www.imsglobal.org/xsd/imslticm_v1p0" xmlns:lticp="http://www.imsglobal.org/xsd/imslticp_v1p0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.imsglobal.org/xsd/imslticc_v1p0" xmlns:blti="http://www.imsglobal.org/xsd/imsbasiclti_v1p0" xsi:schemaLocation="http://www.imsglobal.org/xsd/imslticc_v1p0 http://www.imsglobal.org/xsd/lti/ltiv1p0/imslticc_v1p0.xsd
                          http://www.imsglobal.org/xsd/imsbasiclti_v1p0 http://www.imsglobal.org/xsd/lti/ltiv1p0/imsbasiclti_v1p0p1.xsd
                          http://www.imsglobal.org/xsd/imslticm_v1p0 http://www.imsglobal.org/xsd/lti/ltiv1p0/imslticm_v1p0.xsd
                          http://www.imsglobal.org/xsd/imslticp_v1p0 http://www.imsglobal.org/xsd/lti/ltiv1p0/imslticp_v1p0.xsd">
    <blti:title>{title}</blti:title>
    <blti:launch_url>{"http://localhost:9000/lti/assignment/launch/"+itemId}
    </blti:launch_url>
    <blti:vendor>
    <lticp:code>unknown</lticp:code>
    <lticp:name>unknown</lticp:name>
    </blti:vendor>
    <blti:custom>
    </blti:custom>
    <blti:extensions platform="canvas.instructure.com">
    <lticm:property name="tool_id">grade_passback</lticm:property>
    <lticm:property name="privacy_level">name_only</lticm:property>
    </blti:extensions>
    </cartridge_basiclti_link>
    if (description.isDefined){
      outer = new Elem(outer.prefix,outer.label,outer.attributes, outer.scope,
        (outer.child ++ <blti:description>{description.get}</blti:description>) : _*)
    }
    outer
  }
}
