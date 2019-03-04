package org.corespring.v2.player.cdn

import org.corespring.common.url.EncodingHelper
import play.api.Logger
import play.api.mvc.RequestHeader

class CdnItemAssetResolver(cdnResolver: CdnResolver) extends ItemAssetResolver {

  private val logger = Logger(classOf[CdnItemAssetResolver])

  val helper = new EncodingHelper

  override def bypass(request: RequestHeader): Boolean = {
    val ua = request.headers.toMap.get("User-Agent").map(s => s.mkString)
    logger.info(s"[bypass] user agent: ${ua}")
    ua == "Amazon CloudFront"
  }

  override def resolve(itemId: String)(file: String): String = {
    logger.info(s"[bypass] user agent: ${itemId}")
    cdnResolver.resolveDomain(mkPath(itemId)(helper.encodedOnce(file)))
  }

}
