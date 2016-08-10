package org.corespring.v2.player.cdn

import org.corespring.models.item.PlayerDefinition
import org.corespring.models.item.resource.{BaseFile, StoredFile}
import org.corespring.models.json.JsonFormatting
import org.corespring.v2.player.hooks.PlayerItemProcessor
import play.api.Logger
import play.api.libs.json._

import scala.util.matching.Regex
import org.corespring.macros.DescribeMacro.{describe => ds}

import scala.collection.mutable

/**
 * Add cdn to all files used in the item
 * Return a reduced json which contains only relevant properties
 */
class CdnPlayerItemProcessor(
  itemAssetResolver: ItemAssetResolver,
  jsonFormatting: JsonFormatting) extends PlayerItemProcessor {

  import org.corespring.common.json.JsonStringReplace._

  private val logger = Logger(this.getClass)

  def makePlayerDefinitionJson(session: JsValue, playerDefinition: Option[PlayerDefinition]): JsValue = {
    require(playerDefinition.isDefined, "playerDefinition cannot be empty")
    val storedFiles = playerDefinition.get.files.filter(_.isInstanceOf[StoredFile])
    val playerDefinitionJson = reducedPlayerDefinitionJson(playerDefinition.get)

    if (storedFiles.length == 0) {
      playerDefinitionJson
    } else {
      val maybeItemId = (session \ "itemId").asOpt[String]
      if (!maybeItemId.isDefined) {
        playerDefinitionJson
      } else {
        val resolveCache = new mutable.HashMap[String,String]()

        def resolve(file:String):String = {
          resolveCache.getOrElseUpdate(file, itemAssetResolver.resolve(maybeItemId.get)(file))
        }

        def replacers(file: BaseFile) = Seq(
          (input: String) => new Regex("\"" + file.name + "\"").replaceAllIn(input, m => "\"" + resolve(file.name) + "\""),
          (input: String) => new Regex("^" + file.name + "$").replaceAllIn(input, m => resolve(file.name)))

        replaceStringsInJson(playerDefinitionJson) { (src) =>
          storedFiles.foldLeft(src) { (src, file) =>
            replacers(file).foldLeft(src) { (src, replacer) =>
              replacer(src)
            }
          }
        }
      }
    }
  }

  //Ensure that only the requested properties are returned
  private def reducedPlayerDefinitionJson(playerDefinition: PlayerDefinition): JsValue = {
    val v2Json = Json.toJson(playerDefinition)(jsonFormatting.formatPlayerDefinition)
    Json.obj(
      "xhtml" -> (v2Json \ "xhtml").as[String],
      "components" -> (v2Json \ "components").as[JsValue],
      "summaryFeedback" -> (v2Json \ "summaryFeedback").as[String])
  }

}

