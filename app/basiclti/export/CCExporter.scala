package basiclti.export

import org.bson.types.ObjectId
import java.io._
import java.util.zip.{ZipEntry, ZipOutputStream}
import scala.Some
import collection.mutable.ArrayBuffer
import xml.{XML, Elem}

import basiclti.controllers.routes.{AssignmentLauncher => AssignmentLauncherRoutes}


object CCExporter {


  private class ConfigFiles {
    val virtualFiles = new ArrayBuffer[VirtualFile]()
    val resources = new ArrayBuffer[CCResource]()
  }

  def packageItems(ids: Seq[String], domain: String): Array[Byte] = {
    val cf = new ConfigFiles
    cf.virtualFiles += CCFolder("course_settings/")
    buildLti(ids, cf, domain)
    buildAssignmentStuff(ids, cf, domain)
    buildManifest(cf)
    zip(cf.virtualFiles)
  }

  private def buildLti(ids: Seq[String], cf: ConfigFiles, domain : String) {
    var count = 1
    ids.foreach(id => {
      val url = domain + AssignmentLauncherRoutes.launch(new ObjectId(id))
      val content = xmlToString(LtiItemOutcomes(url, "item" + count, None).toXml)
      cf.virtualFiles += LtiFile("item" + count + ".xml", "item" + count, content)
      cf.resources += CCLtiResource(IdGen(), Seq(CCResourceFile("item" + count + ".xml")))
      count += 1
    })
  }

  private def buildAssignmentStuff(ids: Seq[String], cf: ConfigFiles, domain: String) {
    val ag = CCAssignmentGroup(IdGen(), "Assignments")
    val ags = CCAssignmentGroups(Seq(ag))
    cf.virtualFiles += CCFile("course_settings/assignment_groups.xml", xmlToString(ags.toXml))
    cf.virtualFiles += CCFile("course_settings/canvas_export.txt", "Q: What did the panda say when he was forced out of his natural habitat?\nA: This is un-BEAR-able")
    cf.resources += CCWebFolderResource(IdGen(), "course_settings/canvas_export.txt",
      Seq(CCResourceFile("course_settings/canvas_export.txt"), CCResourceFile("course_settings/assignment_groups.xml")))
    var count = 0
    ids.foreach(id => {
      val folderId = IdGen()
      count += 1
      val externalToolUrl = domain + AssignmentLauncherRoutes.launch(new ObjectId(id))
      val as = CCExternalToolAssignmentSettings(folderId, "Assignment " + count, ag.identifier, externalToolUrl)
      cf.virtualFiles += CCFile(folderId + "/assignment_settings.xml", xmlToString(as.toXml))
      val ha = CCHtmlAssignment("Assignment " + count)
      cf.virtualFiles += CCFile(folderId + "/assignment-" + count + ".html", ha.toString);
      cf.resources += CCWebFolderResource(folderId,
        folderId + "/assignment-" + count + ".html",
        Seq(CCResourceFile(folderId + "/assignment-" + count + ".html"), CCResourceFile(folderId + "/assignment_settings.xml"))
      )
    })
  }

  private def buildManifest(cf: ConfigFiles) = {
    val name = "imsmanifest.xml"
    val organizations = Seq(
      CCOrganization(IdGen(),
        Some(CCItemGroup("", "LearningModules", Seq()))
      ))
    val ccmanifest = CCManifest(IdGen(),
      cf.resources.toSeq,
      organizations
    )
    cf.virtualFiles += CCFile(name, xmlToString(ccmanifest.toXml))
  }

  private def zip(files: Iterable[VirtualFile]): Array[Byte] = {
    val byteOutStream = new ByteArrayOutputStream()
    val zip = new ZipOutputStream(byteOutStream)
    files.foreach {
      lf =>
        zip.putNextEntry(new ZipEntry(lf.name))
        if (!lf.isDirectory) zip.write(lf.content.getBytes)
        zip.closeEntry()
    }
    zip.close()
    byteOutStream.toByteArray
  }

  private trait VirtualFile {
    val name: String
    val content: String
    val isDirectory: Boolean
  }

  private case class CCFolder(name: String) extends VirtualFile {
    override val content = ""
    override val isDirectory = true
  }

  private case class CCFile(name: String, content: String) extends VirtualFile {
    override val isDirectory = false
  }

  private case class LtiFile(name: String, identifier: String, content: String) extends VirtualFile {
    override val isDirectory = false
  }

  private object IdGen {
    def apply(): String = (new ObjectId()).toString
  }

  private def xmlToString(xml: Elem): String = {
    val writer = new StringWriter()
    XML.write(writer, xml, "utf-8", true, null)
    writer.flush()
    writer.toString
  }
}
