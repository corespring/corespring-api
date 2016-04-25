package org.corespring.v2.auth.wired

import org.corespring.conversion.qti.transformers.ItemTransformer
import org.corespring.models.item.{Item, PlayerDefinition}
import org.corespring.models.json.JsonFormatting
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.auth.PlayerDefinitionLoader
import org.corespring.v2.auth.models.{OrgAndOpts, PlayerAccessSettings}
import org.corespring.v2.errors.Errors.noItemIdInSession
import org.corespring.v2.errors.V2Error
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue}

import scalaz.Scalaz._
import scalaz.{Success, Validation}

trait HasPermissions {
  def has(itemId: String, sessionId: Option[String], settings: PlayerAccessSettings): Validation[V2Error, Boolean]
}

class PlayerDefinitionLoaderWired(
  itemTransformer: ItemTransformer,
  jsonToPlayerDef: JsonFormatting,
  itemAuth: ItemAuth[OrgAndOpts],
  perms: HasPermissions) extends PlayerDefinitionLoader {

  lazy val logger = Logger(classOf[PlayerDefinitionLoaderWired])

  def loadPlayerDefinition(sessionId: String, session: JsValue)(implicit identity: OrgAndOpts): Validation[V2Error, PlayerDefinition] = {

    def loadContentItem: Validation[V2Error, Item] = {
      for {
        itemId <- (session \ "itemId").asOpt[String].toSuccess(noItemIdInSession(sessionId))
        item <- itemAuth.loadForRead(itemId)
        hasPerms <- perms.has(item.id.toString, Some(sessionId), identity.opts)
      } yield item
    }

    val sessionPlayerDef: Option[PlayerDefinition] = (session \ "item").asOpt[JsObject].map {
      internalModel =>
        jsonToPlayerDef.toPlayerDefinition(internalModel)
    }.flatten

    sessionPlayerDef
      .map { d => Success(d) }
      .getOrElse {
        loadContentItem.map { i =>
          itemTransformer.createPlayerDefinition(i)
        }
      }
  }
}
