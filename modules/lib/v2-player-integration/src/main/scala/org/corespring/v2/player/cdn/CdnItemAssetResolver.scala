package org.corespring.v2.player.cdn

import org.corespring.common.url.EncodingHelper
import play.api.Logger
import play.api.mvc.RequestHeader
import org.corespring.macros.DescribeMacro.describe

class CdnItemAssetResolver(cdnResolver: CdnResolver) extends ItemAssetResolver {

  private val logger = Logger(classOf[CdnItemAssetResolver])

  private val AMAZON_CLOUDFRONT = "Amazon CloudFront"

  val helper = new EncodingHelper

  override def bypass(request: RequestHeader): Boolean = {
    logger.info(describe(request))
    val headerMap = request.headers.toMap
    logger.info(describe(headerMap))
    val ua = request.headers.toMap.get("User-Agent").map(s => s.mkString)
    ua match {
      case Some(a) => a == AMAZON_CLOUDFRONT
      case _ => false
    }
  }

  override def resolve(itemId: String)(file: String): String = {
    logger.info(describe(itemId))
    cdnResolver.resolveDomain(mkPath(itemId)(helper.encodedOnce(file)))
  }

}

