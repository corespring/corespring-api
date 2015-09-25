import bootstrap.Main
import com.amazonaws.services.s3.AmazonS3
import filters.{AccessControlFilter, AjaxFilter, IEHeaders}
import org.corespring.common.config.AppConfig
import org.corespring.container.client.filters.CheckS3CacheFilter
import org.corespring.play.utils.{CallBlockOnHeaderFilter, ControllerInstanceResolver}
import org.corespring.web.common.views.helpers.BuildInfo
import play.api.GlobalSettings
import play.api.mvc.{Controller, EssentialAction, Filters, WithFilters}

import scala.concurrent.ExecutionContext

object NewGlobal
  extends WithFilters(
    CallBlockOnHeaderFilter,
    AjaxFilter,
    AccessControlFilter,
    IEHeaders)
  with ControllerInstanceResolver
  with GlobalSettings {
  lazy val controllers: Seq[Controller] = Main.controllers


  lazy val componentSetFilter = new CheckS3CacheFilter {
    override implicit def ec: ExecutionContext = ExecutionContext.global

    override lazy val bucket: String = AppConfig.assetsBucket

    override def appVersion: String = BuildInfo.commitHashShort

    override def s3: AmazonS3 = bootstrap.Main.s3

    override def intercept(path: String) = path.contains("component-sets")

  }

  override def doFilter(a: EssentialAction): EssentialAction = {
    Filters(super.doFilter(a), Seq(componentSetFilter): _*)
  }
}
