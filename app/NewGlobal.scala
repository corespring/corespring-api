import filters.{IEHeaders, AccessControlFilter, AjaxFilter}
import org.corespring.models.json.JsonFormatting
import org.corespring.play.utils.{ControllerInstanceResolver, CallBlockOnHeaderFilter}
import org.corespring.services.item.ItemService
import org.corespring.v2.api.ItemSessionApi
import org.corespring.v2.api.drafts.item.ItemDrafts
import org.corespring.drafts.item.{ ItemDrafts => DraftsBackend }
import org.corespring.v2.api.drafts.item.json.ItemDraftJson
import play.api.GlobalSettings
import play.api.mvc.{Controller, WithFilters}

/*trait V2ApiModule {
  import com.softwaremill.macwire._

  def itemService : ItemService// = ???
  def jsonFormatting : JsonFormatting
  lazy val itemDraftJson = wire[ItemDraftJson]
  lazy val itemDrafts = wire[DraftsBackend]
  lazy val itemDraftsApi = wire[ItemDrafts]
}*/

object NewGlobal
  extends WithFilters(
    CallBlockOnHeaderFilter,
    AjaxFilter,
    AccessControlFilter,
    IEHeaders)
  with ControllerInstanceResolver
  with GlobalSettings{


  lazy val coreServices = new

  override def controllers: Seq[Controller] = Seq.empty
}
