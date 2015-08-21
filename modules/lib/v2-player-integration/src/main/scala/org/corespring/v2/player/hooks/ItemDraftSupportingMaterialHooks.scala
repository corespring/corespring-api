package org.corespring.v2.player.hooks

import org.corespring.container.client.hooks.Hooks.R
import org.corespring.container.client.hooks.{ CreateNewMaterialRequest, File, Binary, SupportingMaterialHooks }
import play.api.libs.json.JsValue
import play.api.mvc.{ SimpleResult, RequestHeader }

trait ItemDraftSupportingMaterialHooks extends SupportingMaterialHooks {
  override def create[F <: File](id: String, sm: CreateNewMaterialRequest[F])(implicit h: RequestHeader): R[JsValue] = ???

  override def deleteAsset(id: String, name: String, filename: String)(implicit h: RequestHeader): R[JsValue] = ???

  override def addAsset(id: String, name: String, binary: Binary)(implicit h: RequestHeader): R[JsValue] = ???

  override def delete(id: String, name: String)(implicit h: RequestHeader): R[JsValue] = ???

  override def getAsset(id: String, name: String, filename: String)(implicit h: RequestHeader): SimpleResult = ???

  override def updateContent(id: String, name: String, filename: String, content: String)(implicit h: RequestHeader): R[JsValue] = ???
}
