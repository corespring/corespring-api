package basiclti.export

import org.bson.types.ObjectId
import java.io._
import java.util.zip.{ ZipEntry, ZipOutputStream }
import scala.Some


object LtiExporter {

  def packageItems(ids: Seq[ObjectId]):Array[Byte] = {
    val resources = ids.map(id => buildLtiFile(id.toString()))
    val manifest = buildManifest(resources)
    zip("item_player.imscc",resources :+ manifest)
  }
  private def buildLtiFile(id: String):LtiFile = {
    val name = "item"+id
    val content = LtiItemLaunch(id, name, None).toXml.toString;
    LtiFile(name+".xml",name,content)
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
      zip.write(lf.content.getBytes);
      zip.closeEntry()
    }
    zip.close()
    byteOutStream.toByteArray
  }
  private trait VirtualFile{
    val name: String
    val content:String
  }
  private case class CCFile(name: String, content: String) extends VirtualFile
  private case class LtiFile(name: String, identifier: String, content: String) extends VirtualFile
  private object IdGen{
    private var id = 0;
    def apply(prefix:String = ""):String = {
      id = id + 1;
      prefix+id.toString
    }
  }
}
