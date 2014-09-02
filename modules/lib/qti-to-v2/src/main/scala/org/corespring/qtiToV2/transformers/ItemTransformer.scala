package org.corespring.qtiToV2.transformers

import org.bson.types.ObjectId
import org.corespring.common.json.{ JsonCompare, JsonTransformer }
import org.corespring.platform.core.models.Standard
import org.corespring.platform.core.models.item.resource.{ CDataHandler, Resource, VirtualFile, XMLCleaner }
import org.corespring.platform.core.models.item.{ Item, ItemTransformationCache, PlayerDefinition }
import org.corespring.platform.core.services.BaseFindAndSaveService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.QtiTransformer
import play.api.{ Configuration, Play, Logger }
import play.api.libs.json.{ JsObject, JsString, JsValue, Json }

trait ItemTransformer {

  def configuration: Configuration
  def useCache: Boolean = configuration.getBoolean("v2.itemTransformer.useCache").getOrElse(true)
  def checkCacheIsUpToDate: Boolean = configuration.getBoolean("v2.itemTransformer.checkCacheIsUpToDate").getOrElse(false)

  lazy val logger = Logger("org.corespring.qtiToV2.ItemTransformer")

  logger.trace(s"useCache=$useCache")

  def cache: ItemTransformationCache
  def itemService: BaseFindAndSaveService[Item, VersionedId[ObjectId]]

  def updateV2Json(itemId: VersionedId[ObjectId]): Option[Item] = {

    logger.debug(s"itemId=${itemId} function=updateV2Json#VersionedId[ObjectId]")
    itemService.findOneById(itemId) match {
      case Some(item) => item.playerDefinition match {
        case None => try {
          updateV2Json(item)
        } catch {
          case e: Exception => {
            e.printStackTrace
            None
          }
        }
        case _ => Some(item)
      }
      case _ => None
    }
  }

  def updateV2Json(item: Item): Option[Item] = {
    logger.debug(s"itemId=${item.id} function=updateV2Json#Item")
    transformToV2Json(item, Some(createFromQti(item))).asOpt[PlayerDefinition]
      .map(playerDefinition => item.copy(playerDefinition = Some(playerDefinition))) match {
        case Some(updatedItem) => item.playerDefinition.equals(updatedItem.playerDefinition) match {
          case true => Some(updatedItem)
          case _ => {
            logger.trace(s"itemId=${item.id} function=updateV2Json#Item - saving item")
            itemService.save(updatedItem)
            Some(updatedItem)
          }
        }
        case _ => None
      }
  }

  def transformToV2Json(item: Item): JsValue = transformToV2Json(item, None)

  def transformToV2Json(item: Item, rootJson: Option[JsObject]): JsValue = {
    logger.debug(s"itemId=${item.id} function=transformToV2Json")
    logger.trace(s"itemId=${item.id} function=transformToV2Json -> rootJson=${rootJson.map(Json.stringify)}")
    implicit val ResourceFormat = Resource.Format

    val root: JsObject = (rootJson match {
      case Some(json) => json
      case None => {

        val itemPlayerDef: Option[JsObject] = item.playerDefinition.map(Json.toJson(_).as[JsObject])
        if (checkCacheIsUpToDate) {
          val rawMappedThroughPlayerDef = createFromQti(item).asOpt[PlayerDefinition].map(Json.toJson(_))
          for {
            rawPd <- rawMappedThroughPlayerDef
            itemPd <- itemPlayerDef
          } yield {
            compareJson(item.id, "createdFromQti-vs-item.playerDefinition", rawPd, itemPd)
          }
        }
        itemPlayerDef.getOrElse(createFromQti(item))
      }
    })
    val profile = toProfile(item)
    val out = root ++ Json.obj(
      "profile" -> profile,
      "supportingMaterials" -> Json.toJson(item.supportingMaterials))

    logger.trace(s"itemId=${item.id} function=transformToV2Json json=${Json.stringify(out)}")
    out
  }

  private def toProfile(item: Item): JsValue = {
    val taskInfoJson = item.taskInfo.map { info =>
      Json.obj("taskInfo" -> mapTaskInfo(Json.toJson(info)))
    }.getOrElse(Json.obj("taskInfo" -> Json.obj()))
    val contributorDetails = item.contributorDetails.map { details =>
      Json.obj("contributorDetails" -> Json.toJson(details))
    }.getOrElse(Json.obj("contributorDetails" -> Json.obj()))
    val otherAlignments = item.otherAlignments.map { alignments =>
      Json.obj("otherAlignments" -> Json.toJson(alignments))
    }.getOrElse(Json.obj("otherAlignments" -> Json.obj()))

    taskInfoJson ++
      contributorDetails ++
      otherAlignments ++
      Json.obj(
        "standards" -> item.standards.map(Standard.findOneByDotNotation).flatten.map(Json.toJson(_)),
        "reviewsPassed" -> item.reviewsPassed,
        "reviewsPassedOther" -> item.reviewsPassedOther,
        "priorGradeLevel" -> item.priorGradeLevels,
        "priorUse" -> item.priorUse,
        "priorUseOther" -> item.priorUseOther,
        "lexile" -> item.lexile)
  }

  def mapTaskInfo(taskInfoJson: JsValue): JsValue = {
    val tf = new JsonTransformer(
      "primarySubject" -> "subjects.primary",
      "relatedSubject" -> "subjects.related") {}
    tf.transform(taskInfoJson)
  }

  private def createFromQti(item: Item): JsObject = {
    val transformedJson = getTransformation(item)

    Json.obj(
      "metadata" -> Json.obj(
        "title" -> JsString(item.taskInfo.map(_.title.getOrElse("?")).getOrElse("?"))),
      "files" -> (item.data match {
        case Some(data) => data.files
          .filter(f => f.name != "qti.xml")
          .map(f => Json.obj("name" -> f.name, "contentType" -> f.contentType))
        case _ => Seq.empty[JsObject]
      })) ++ transformedJson.as[JsObject]
  }

  private def compareJson(itemId: VersionedId[ObjectId], msg: String, a: JsValue, b: JsValue) = {
    JsonCompare.caseInsensitiveSubTree(a, b) match {
      case Left(diffs) => {
        diffs.foreach { d =>
          logger.warn(s"itemId=${itemId} msg=$msg function=compareCache json diff: $d")
          logger.trace(s"itemId=$itemId a=${Json.prettyPrint(a)}")
          logger.trace(s"itemId=$itemId b=${Json.prettyPrint(b)}")
        }
      }
      case Right(_) => logger.debug(s"itemId=${itemId} msg=$msg function=compareCache - json in cache is up to date")
    }
  }

  private def getTransformation(item: Item): JsValue = {

    def generate(addToCache: Boolean) = {
      logger.debug(s"itemId=${item.id} function=getTransformation - generating json")
      val qti = for {
        data <- item.data
        qti <- data.files.find(_.name == "qti.xml")
      } yield qti.asInstanceOf[VirtualFile]
      require(qti.isDefined, s"item: ${item.id} has no qti xml")
      val transformedJson = QtiTransformer.transform(scala.xml.XML.loadString(XMLCleaner.clean(CDataHandler.addCDataTags(qti.get.content))))
      if (addToCache) {
        logger.debug(s"itemId=${item.id} function=getTransformation - call cache.setCachedTransformation")
        cache.setCachedTransformation(item, transformedJson)
      }
      logger.trace(s"itemId=${item.id} function=getTransformation generatedJson=${Json.stringify(transformedJson)}")
      transformedJson
    }

    cache.getCachedTransformation(item) match {
      case Some(json: JsValue) => {
        if (useCache) {
          logger.debug(s"itemId=${item.id} function=getTransformation - found cachedJson")
          logger.trace(s"itemId=${item.id} function=getTransformation cachedJson=${Json.stringify(json)}")

          if (checkCacheIsUpToDate) {
            val generated = generate(false)
            compareJson(item.id, "generated-vs-cached", generated, json)
          }
          json
        } else generate(useCache)
      }
      case _ => generate(useCache)
    }
  }

}
