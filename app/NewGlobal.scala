import bootstrap.Main
import filters.{ IEHeaders, AccessControlFilter, AjaxFilter }
import org.corespring.play.utils.{ ControllerInstanceResolver, CallBlockOnHeaderFilter }
import play.api.{ GlobalSettings }
import play.api.mvc.{ Controller, WithFilters }

object NewGlobal
  extends WithFilters(
    CallBlockOnHeaderFilter,
    AjaxFilter,
    AccessControlFilter,
    IEHeaders)
  with ControllerInstanceResolver
  with GlobalSettings {
  lazy val controllers: Seq[Controller] = Main.controllers
}
