package org.corespring.v2.player.hooks

import org.corespring.container.client.hooks.{ Binary, _ }
import org.corespring.platform.core.models.item.resource.{ BaseFile, Resource, StoredFile, VirtualFile }

private[hooks] trait MaterialToResource {

  protected def binaryToFile(b: Binary, isMain: Boolean = false) = StoredFile(name = b.name, isMain = isMain, contentType = b.mimeType)
  protected def htmlToFile(h: Html, isMain: Boolean = false) = VirtualFile(name = h.name, isMain = isMain, contentType = h.mimeType, content = h.content)
  protected def requestToFile[F <: File](sm: CreateNewMaterialRequest[F]): BaseFile = sm match {
    case CreateBinaryMaterial(_, _, binary) => binaryToFile(binary)
    case CreateHtmlMaterial(_, _, main, _) => htmlToFile(main)
  }

  def materialToResource[F <: File](sm: CreateNewMaterialRequest[F]): Resource = Resource(
    name = sm.name,
    materialType = Some(sm.materialType),
    files = Seq(requestToFile(sm)))
}
