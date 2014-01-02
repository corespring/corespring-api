package org.corespring.lti.export

import xml.Elem

private object Helper {
  def addChildNode(e: Elem, child: Elem*): Elem = {
    val children = e.child ++ child
    Elem(e.prefix, e.label, e.attributes, e.scope, true, children: _*)
  }
}

case class CCManifest(identifier: String, resources: Seq[CCResource], organizations: Seq[CCOrganization]) {
  private val resourcesXml: Option[Elem] = {
    val outer: Elem = <resources></resources>
    if (!resources.isEmpty) {
      val out = Helper.addChildNode(outer, resources.map(_.toXml): _*)
      Some(out)
    } else None
  }
  private val organizationsXml: Option[Elem] = {
    val outer: Elem = <organizations></organizations>
    if (!organizations.isEmpty) {
      Some(Helper.addChildNode(outer, organizations.map(_.toXml): _*))
    } else None
  }
  def toXml: Elem = {
    var outer: Elem = <manifest identifier={ identifier } xmlns="http://www.imsglobal.org/xsd/imsccv1p2/imscp_v1p1" xmlns:lom="http://ltsc.ieee.org/xsd/imsccv1p2/LOM/resource" xmlns:lomimscc="http://ltsc.ieee.org/xsd/imsccv1p2/LOM/manifest" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="
  http://www.imsglobal.org/xsd/imsccv1p2/imscp_v1p1
  http://www.imsglobal.org/profile/cc/ccv1p2/ccv1p2_imscp_v1p1_v1p0.xsd
  http://ltsc.ieee.org/xsd/imsccv1p2/LOM/resource http://www.imsglobal.org/profile/cc/ccv1p2/LOM/ccv1p2_lomresource_v1p0.xsd
  http://ltsc.ieee.org/xsd/imsccv1p2/LOM/manifest http://www.imsglobal.org/profile/cc/ccv1p2/LOM/ccv1p2_lommanifest_v1p0.xsd">
                        <metadata>
                          <schema>IMS Common Cartridge</schema>
                          <schemaversion>1.2.0</schemaversion>
                        </metadata>
                      </manifest>
    if (organizationsXml.isDefined) {
      outer = Helper.addChildNode(outer, organizationsXml.get)
    }
    if (resourcesXml.isDefined) {
      outer = Helper.addChildNode(outer, resourcesXml.get)
    }
    outer
  }
}

case class CCOrganization(identifier: String, itemGroup: Option[CCItemGroup]) {
  def toXml: Elem = {
    val outer: Elem =
      <organization identifier={ identifier } structure="rooted-hierarchy"></organization>
    if (itemGroup.isDefined) {
      Helper.addChildNode(outer, itemGroup.get.toXml)
    } else outer
  }
}

case class CCItemGroup(title: String, identifier: String, items: Seq[CCItem]) {
  def toXml: Elem = {
    var outer: Elem = <item identifier={ identifier }>
                      </item>
    if (title != "") outer = Helper.addChildNode(outer, <title>{ title }</title>)
    outer = Helper.addChildNode(outer, items.map(_.toXml): _*)
    outer
  }
}
case class CCItem(title: String, identifier: String, identifierref: String) {
  def toXml: Elem = {
    var outer: Elem = <item identifier={ identifier }>
                      </item>
    if (title != "") outer = Helper.addChildNode(outer, <title>{ title }</title>)
    outer
  }
}

trait CCResource {
  val identifier: String
  val rtype: String
  val files: Seq[CCResourceFile]
  def toXml: Elem = {
    val outer: Elem = <resource identifier={ identifier } type={ rtype }></resource>
    Helper.addChildNode(outer, files.map(_.toXml): _*)
  }
}
trait CCHrefResource extends CCResource {
  val href: String
  override def toXml: Elem = {
    val outer: Elem = <resource href={ href } identifier={ identifier } type={ rtype }></resource>
    Helper.addChildNode(outer, files.map(_.toXml): _*)
  }
}
case class CCFolderResource(identifier: String, files: Seq[CCResourceFile]) extends CCResource {
  val rtype = "associatedcontent/imscc_xmlv1p1/learning-application-resource"
}
case class CCWebFolderResource(identifier: String, href: String, files: Seq[CCResourceFile]) extends CCHrefResource {
  val rtype = "associatedcontent/imscc_xmlv1p1/learning-application-resource"
}
case class CCWebResource(identifier: String, href: String, files: Seq[CCResourceFile]) extends CCHrefResource {
  val rtype = "webcontent"
}
case class CCLtiResource(identifier: String, files: Seq[CCResourceFile]) extends CCResource {
  val rtype = "imsbasiclti_xmlv1p0"
}
case class CCResourceFile(href: String) {
  def toXml: Elem = <file href={ href }/>
}

