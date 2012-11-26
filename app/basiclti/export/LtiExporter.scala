package basiclti.export

import org.bson.types.ObjectId
import java.io._
import java.util.zip.{ ZipEntry, ZipOutputStream }
import scala.Some


object LtiExporter {
  def packageItems(ids: Seq[ObjectId]):Array[Byte] = {
    val resources = ids.map(id => buildLtiFile(id.toString()))
    val ag = CCAssignmentGroup(IdGen("ag"),"Assignments")
    val manifest = buildManifest(resources)
    val course_settings = CCFolder("course_settings")
    val ags = buildAssignmentGroups(Seq(ag))
    zip("item_player.imscc",resources ++ Seq(manifest))
  }
  private def buildLtiFile(id: String):LtiFile = {
    val name = "item"+id
    val content = LtiItemLaunch(id, name, None).toXml.toString;
    LtiFile(name+".xml",name,content)
  }
  private def buildAssignmentGroups(groups:Seq[CCAssignmentGroup]):CCFile = {
    val ag = CCAssignmentGroups(groups)
    CCFile("course_settings/assignment_groups.xml",ag.toXml.toString())
  }
  private def buildAssignmentSettings(folderId:String, title:String, assignmentGroupId:String, launchId:String):VirtualFile = {
    val externalToolUrl = "http://localhost:9000/lti/assignment/launch/"+launchId
    val as = CCExternalToolAssignmentSettings(IdGen("etas"),title,assignmentGroupId,externalToolUrl)
    CCFile(folderId+"/assignment_settings.xml",as.toXml.toString())
  }
  private def buildHtmlAssignment(folderId:String, title:String,count:Int):VirtualFile = {
    val ha = CCHtmlAssignment(title)
    CCFile(folderId+"/assignment-"+count.toString+".xml",ha.toString)
  }
  private def buildManifest(resources: Seq[LtiFile]):CCFile = {
    val name = "imsmanifest.xml"
    val ccresources:Seq[CCResource] = resources.map(resource => {
      CCLtiResource(resource.identifier,Seq(CCResourceFile(resource.name)))
    })
    val organizations = Seq(
      CCOrganization(IdGen("ccorg"),
        Some(CCItemGroup("Item Launcher", IdGen("itemgroup"),ccresources.map(ccr => CCItem("Item Player",IdGen("item"),ccr.identifier))))
      ))
    val ccmanifest = CCManifest(IdGen("ccmanifest"),
      ccresources,
      organizations
    )
    CCFile(name, ccmanifest.toXml.toString())
  }
  private def zip(out: String, files: Iterable[VirtualFile]):Array[Byte] = {
    val byteOutStream = new ByteArrayOutputStream()
    val zip = new ZipOutputStream(byteOutStream)
    files.foreach { lf =>
      zip.putNextEntry(new ZipEntry(lf.name))
      if (!lf.isDirectory) zip.write(lf.content.getBytes);
      zip.closeEntry()
    }
    zip.close()
    byteOutStream.toByteArray
  }
  private trait VirtualFile{
    val name: String
    val content:String
    val isDirectory:Boolean
  }
  private case class CCFolder(name: String) extends VirtualFile{
    override val content = "";
    override val isDirectory = true;
  }
  private case class CCFile(name: String, content: String) extends VirtualFile{
    override val isDirectory = false;
  }
  private case class LtiFile(name: String, identifier: String, content: String) extends VirtualFile{
    override val isDirectory = false;
  }
  private object IdGen{
    private var id = 0;
    def apply(prefix:String = ""):String = {
      id = id + 1;
      prefix+id.toString
    }
  }
}
