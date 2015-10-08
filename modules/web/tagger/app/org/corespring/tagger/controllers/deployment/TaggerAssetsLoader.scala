package org.corespring.tagger.controllers.deployment

import com.ee.assets.Loader
import org.corespring.common.log.PackageLogging
import play.api.Play

class TaggerAssetsLoader(val loader: Loader) extends PackageLogging {

  def init(implicit app: play.api.Application) = if (Play.isProd) {
    logger.debug("running S3 deployments...")
    tagger
  }

  def tagger = loader.scripts("tagger")("js/corespring/tagger")

}

object TaggerAssetsLoaderImpl extends TaggerAssetsLoader(loader)