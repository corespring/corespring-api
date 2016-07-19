package org.corespring.v2.player.hooks

import org.corespring.container.client.hooks.Hooks.R
import org.corespring.conversion.qti.transformers.PlayerJsonToItem
import org.corespring.models.item.{ PlayerDefinition, Item }
import org.corespring.models.json.JsonFormatting
import play.api.Logger
import play.api.libs.json.{ Json, JsObject, JsValue }
import play.api.mvc.RequestHeader

import scala.concurrent.Future
import scalaz.Scalaz._

trait BaseItemHooks
  extends org.corespring.container.client.hooks.CoreItemHooks {

  def playerJsonToItem: PlayerJsonToItem
  def jsonFormatting: JsonFormatting

  implicit val formatPlayerDefinition = jsonFormatting.formatPlayerDefinition

  private val logger = Logger(classOf[BaseItemHooks])

  protected def update(id: String, json: JsValue, updateFn: (Item, JsValue) => Item)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]]

  private def baseDefinition(playerDef: Option[PlayerDefinition]): JsObject = Json.toJson(playerDef.getOrElse(PlayerDefinition.empty)).as[JsObject]

  private def savePartOfPlayerDef(id: String, json: JsObject)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = {
    update(id, json, (i, _) => playerJsonToItem.playerDef(i, baseDefinition(i.playerDefinition) ++ json))(header)
  }

  override final def saveXhtml(id: String, xhtml: String)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = {
    savePartOfPlayerDef(id, Json.obj("xhtml" -> xhtml))
  }

  override final def saveCollectionId(id: String, collectionId: String)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = {
    def updateCollectionId(item: Item, json: JsValue): Item = {
      item.copy(collectionId = collectionId)
    }
    update(id, Json.obj("collectionId" -> collectionId), updateCollectionId)
  }

  @deprecated("use SupportingMaterialsService instead", "5.0.0")
  override final def saveSupportingMaterials(id: String, json: JsValue)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = {
    throw new RuntimeException("Not supported")
  }

  override final def saveCustomScoring(id: String, customScoring: String)(implicit header: RequestHeader): R[JsValue] = {

    def updateCustomScoring(item: Item, json: JsValue): Item = {
      val updatedDefinition = item.playerDefinition.map { pd =>
        new PlayerDefinition(pd.files, pd.xhtml, pd.components, pd.summaryFeedback, Some(customScoring), pd.config)
      }.getOrElse {
        PlayerDefinition(Seq.empty, "", Json.obj(), "", Some(customScoring), Json.obj())
      }
      item.copy(playerDefinition = Some(updatedDefinition))
    }

    update(id, Json.obj("customScoring" -> customScoring), updateCustomScoring)
  }

  override final def saveConfigXhtmlAndComponents(id: String, config: JsValue, xhtml: String, components: JsValue)(implicit rh: RequestHeader): Future[Either[(Int, String), JsValue]] = {
    savePartOfPlayerDef(id, Json.obj("config" -> config, "xhtml" -> xhtml, "components" -> components))
  }

  override final def saveComponents(id: String, json: JsValue)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = {
    savePartOfPlayerDef(id, Json.obj("components" -> json))
  }

  override final def saveConfig(id: String, json: JsValue)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = {
    savePartOfPlayerDef(id, Json.obj("config" -> json))
  }

  override final def saveSummaryFeedback(id: String, feedback: String)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = {
    savePartOfPlayerDef(id, Json.obj("summaryFeedback" -> feedback))
  }

  override final def saveProfile(id: String, json: JsValue)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = {
    logger.debug(s"saveProfile id=$id")
    def withKey(j: JsValue) = Json.obj("profile" -> j)
    update(id, json, playerJsonToItem.profile).map { e => e.rightMap(withKey) }
  }

}
