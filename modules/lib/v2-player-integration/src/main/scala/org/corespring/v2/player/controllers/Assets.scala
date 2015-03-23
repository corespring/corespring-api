package org.corespring.v2.player.controllers

import org.corespring.amazon.s3.ConcreteS3Service
import org.corespring.common.config.AppConfig
import org.corespring.container.client.controllers.AssetType.AssetType
import org.corespring.container.client.controllers.{ AssetType, Assets => ContainerAssets }
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.services.item.ItemService
import org.corespring.v2.log.V2LoggerFactory
import play.api.mvc._

import scala.concurrent.Future

trait Assets extends ContainerAssets {

  private lazy val key = AppConfig.amazonKey
  private lazy val secret = AppConfig.amazonSecret
  private lazy val bucket = AppConfig.assetsBucket

  lazy val playS3 = new ConcreteS3Service(key, secret)

  def sessionService: MongoService
  def previewSessionService: MongoService
  def itemService: ItemService

  protected lazy val logger = V2LoggerFactory.getLogger("Assets")

  override def load(t: AssetType, id: String, path: String)(implicit h: RequestHeader): SimpleResult = {
    val decodedPath = java.net.URI.create(path).getPath
    val key = mkKey(t, id, decodedPath)
    logger.debug(s"[load] assetType=${t}, id=$id, path=$path")
    playS3.download(bucket, key, Some(h.headers))
  }

  private def mkKey(t: AssetType, p: String*) = t match {
    case AssetType.Item => p.mkString("/")
    case AssetType.Draft => (t.folderName :+ p).mkString("/")
  }

  override def upload(t: AssetType, id: String, path: String)(block: (Request[Int]) => SimpleResult): Action[Int] = {
    val decodedPath = java.net.URI.create(path).getPath
    val key = mkKey(t, id, decodedPath)
    Action(playS3.upload(bucket, key, (_) => None)) { r => block(r) }
  }

  override def delete(t: AssetType, id: String, path: String)(implicit h: RequestHeader): Future[Option[(Int, String)]] = Future {
    val decodedPath = java.net.URI.create(path).getPath
    val key = mkKey(t, id, decodedPath)
    val response = playS3.delete(bucket, key)
    if (response.success) {
      None
    } else {
      Some(500 -> response.msg)
    }
  }

}

