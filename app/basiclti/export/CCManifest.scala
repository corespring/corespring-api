package basiclti.export

import xml.Elem

/**
 * Created with IntelliJ IDEA.
 * User: josh
 * Date: 11/25/12
 * Time: 2:08 PM
 * To change this template use File | Settings | File Templates.
 */
case class CCManifest(identifier:String, resources:Seq[CCResource], organizations:Seq[CCOrganization]) {
  private val resourcesXml:Option[Elem] = {
    val outer:Elem = <resources></resources>
    if (!resources.isEmpty){
      Some(new Elem(outer.prefix,outer.label,outer.attributes, outer.scope, (outer.child ++ resources.map(_.toXml)) : _*))
    } else None
  }
  private val organizationsXml:Option[Elem] = {
    val outer:Elem = <organizations></organizations>
    if (!organizations.isEmpty){
      Some(new Elem(outer.prefix,outer.label,outer.attributes, outer.scope, (outer.child ++ organizations.map(_.toXml)) : _*))
    } else None
  }
 def toXml:Elem = {
   var outer:Elem = <manifest identifier={identifier}
                            xmlns="http://www.imsglobal.org/xsd/imsccv1p2/imscp_v1p1"
                            xmlns:lom="http://ltsc.ieee.org/xsd/imsccv1p2/LOM/resource"
                            xmlns:lomimscc="http://ltsc.ieee.org/xsd/imsccv1p2/LOM/manifest"
                            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                            xsi:schemaLocation="
  http://www.imsglobal.org/xsd/imsccv1p2/imscp_v1p1
  http://www.imsglobal.org/profile/cc/ccv1p2/ccv1p2_imscp_v1p1_v1p0.xsd
  http://ltsc.ieee.org/xsd/imsccv1p2/LOM/resource http://www.imsglobal.org/profile/cc/ccv1p2/LOM/ccv1p2_lomresource_v1p0.xsd
  http://ltsc.ieee.org/xsd/imsccv1p2/LOM/manifest http://www.imsglobal.org/profile/cc/ccv1p2/LOM/ccv1p2_lommanifest_v1p0.xsd">
   <metadata>
     <schema>IMS Common Cartridge</schema>
     <schemaversion>1.2.0</schemaversion>
   </metadata>
 </manifest>
   if (resourcesXml.isDefined){
     outer = new Elem(outer.prefix, outer.label, outer.attributes, outer.scope, (outer.child ++ resourcesXml.get) : _*)
   }
   if (organizationsXml.isDefined){
     outer = new Elem(outer.prefix, outer.label, outer.attributes, outer.scope, (outer.child ++ organizationsXml.get) : _*)
   }
   outer
 }
}

case class CCOrganization(identifier: String,itemGroup:Option[CCItemGroup]){
  def toXml:Elem = {
    val outer:Elem =
    <organization identifier={identifier} structure="rooted-hierarchy"></organization>
    if (itemGroup.isDefined){
      new Elem(outer.prefix,outer.label,outer.attributes,outer.scope,(outer.child ++ itemGroup.get.toXml) : _*);
    } else outer
  }
}

case class CCItemGroup(title: String, identifier: String, items:Seq[CCItem]){
  def toXml:Elem = {
    val outer:Elem = <item identifier={identifier}>
      <title>{title}</title>
    </item>
    new Elem(outer.prefix,outer.label,outer.attributes,outer.scope, (outer.child ++ items.map(_.toXml)) : _*)
  }
}
case class CCItem(title: String, identifier:String, identifierref:String){
  def toXml:Elem = <item identifier={identifier} identifierref={identifierref}>
    <title>{title}</title>
  </item>
}

trait CCResource{
  val identifier: String
  val rtype: String;
  val files: Seq[CCResourceFile];
  def toXml: Elem;
}
case class CCWebResource(identifier: String, href: String, files: Seq[CCResourceFile]) extends CCResource{
  val rtype = "webcontent"
  def toXml:Elem = {
    val outer:Elem = <resource href={href} identifier={identifier} type={rtype}></resource>
    new Elem(outer.prefix,outer.label, outer.attributes, outer.scope, (outer.child ++ files.map(_.toXml)) : _*)
  }
}
case class CCLtiResource(identifier: String, files: Seq[CCResourceFile]) extends CCResource{
  val rtype = "imsbasiclti_xmlv1p0"
  def toXml:Elem = {
    val outer:Elem = <resource identifier={identifier} type={rtype}></resource>
    new Elem(outer.prefix,outer.label, outer.attributes, outer.scope, (outer.child ++ files.map(_.toXml)) : _*)
  }
}
case class CCResourceFile(href: String){
  def toXml:Elem = <file href={href} />
}
