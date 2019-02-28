package org.corespring.container.client.controllers

import org.corespring.container.client.HasContainerContext
import org.corespring.container.client.hooks.UploadResult
import play.api.mvc._

import scala.concurrent.Future

object AssetType extends Enumeration {
  type AssetType = Val
  protected case class Val(val folderName: String) extends super.Val
  implicit def valueToVal(x: Value) = x.asInstanceOf[Val]
  val Draft = Val("item-drafts")
  val Item = Val("items")
}

trait Assets extends Controller with HasContainerContext {
  import AssetType._
  def load(t: AssetType, id: String, path: String)(implicit h: RequestHeader): SimpleResult
  def delete(t: AssetType, id: String, path: String)(implicit h: RequestHeader): Future[Option[(Int, String)]]
  def upload(t: AssetType, id: String, path: String)(predicate: RequestHeader => Option[SimpleResult]): BodyParser[Future[UploadResult]]
}

