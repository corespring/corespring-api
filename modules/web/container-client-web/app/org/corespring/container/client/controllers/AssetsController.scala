package org.corespring.container.client.controllers

import grizzled.slf4j.Logger
import org.corespring.container.client.HasContainerContext
import org.corespring.container.client.hooks.{ AssetHooks, GetAssetHook }
import org.joda.time.DateTime
import play.api.mvc.{ Action, Controller, RequestHeader, SimpleResult }

import scala.concurrent.Future

trait GetAsset[H <: GetAssetHook] extends Controller with HasContainerContext {

  def hooks: H

  def getFile(id: String, path: String) = Action.async { request =>
    Future { hooks.loadFile(id, path)(request) }
  }
}

private[controllers] trait TimestampPath {

  def getTimestamp: String = DateTime.now.getMillis.toString

  def timestamped(path: String): String = {
    val (dir, basename) = grizzled.file.util.dirnameBasename(path)
    val timestampedName = s"${getTimestamp}-$basename"
    s"${if (dir == ".") "" else s"$dir/"}$timestampedName"
  }
}

trait AssetsController[H <: AssetHooks] extends GetAsset[H] with TimestampPath {

  override def hooks: H

  private val logger = {
    val c = classOf[AssetsController[_]]
    Logger(c)
  }

  def acceptableSuffixes: Seq[String] = Seq(
    "gif",
    "jpeg",
    "jpg",
    "mp3",
    "ogg",
    "png")

  def acceptableType(rh: RequestHeader): Option[SimpleResult] = {
    if (acceptableSuffixes.isEmpty) {
      None
    } else {
      val suffix = if (rh.path.contains(".")) rh.path.split("\\.").lastOption else None

      suffix match {
        case None => Some(BadRequest(s"Unknown file suffix for path: ${rh.path}"))
        case Some(s) => if (acceptableSuffixes.contains(s.toLowerCase)) {
          None
        } else {
          Some(BadRequest(s"Unsupported suffix: $s"))
        }
      }
    }
  }

  def uploadFile(id: String, path: String) = {

    val timestampedPath = timestamped(path)
    logger.debug(s"function=uploadFile, id=$id, path=$path, timestampedPath=$timestampedPath")

    Action.async(hooks.upload(id, timestampedPath)(acceptableType)) { request =>
      request.body.map { uploadResult =>
        Ok(uploadResult.path)
      }
    }
  }

  def deleteFile(id: String, path: String) = Action.async {
    implicit request =>
      hooks.deleteFile(id, path).map { err =>
        err match {
          case None => Ok
          case Some((code, msg)) => Status(code)(msg)
        }
      }
  }
}
