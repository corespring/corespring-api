package org.corespring.qtiToV2.transformers

import org.bson.types.ObjectId
import org.corespring.common.json.{ JsonCompare, JsonTransformer }
import org.corespring.platform.core.models.{ ContentCollection, Standard }
import org.corespring.platform.core.models.item.resource.{ CDataHandler, Resource, VirtualFile, XMLCleaner }
import org.corespring.platform.core.models.item.{ Item, PlayerDefinition }
import org.corespring.platform.core.services.BaseFindAndSaveService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.QtiTransformer
import play.api.{ Configuration, Play, Logger }
import play.api.libs.json.{ JsObject, JsString, JsValue, Json }

trait ItemTransformer {

  def configuration: Configuration

  def checkModelIsUpToDate: Boolean = configuration.getBoolean("v2.itemTransformer.checkModelIsUpToDate").getOrElse(false)

  lazy val logger = Logger("org.corespring.qtiToV2.ItemTransformer")

  //TODO: Remove service - transform should only transform. see: CA-2085
  def itemService: BaseFindAndSaveService[Item, VersionedId[ObjectId]]

  //TODO: Remove service - transform should only transform.
  def loadItemAndUpdateV2(itemId: VersionedId[ObjectId]): Option[Item] = {
    itemService.findOneById(itemId) match {
      case Some(item) if (item.createdByApiVersion == 1) => Some(updateV2Json(item))
      case Some(item) => Some(item)
      case _ => None
    }
  }

  def findCollection(id: ObjectId): Option[ContentCollection]

  def updateV2Json(itemId: VersionedId[ObjectId]): Option[Item] = {

    logger.debug(s"itemId=${itemId} function=updateV2Json#VersionedId[ObjectId]")
    itemService.findOneById(itemId) match {
      case Some(item) => item.playerDefinition match {
        case None => try {
          Some(updateV2Json(item))
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

  //TODO: PR - tidy this up..
  def createPlayerDefinition(item: Item): PlayerDefinition = {
    item.playerDefinition.getOrElse {

      val newDef = createFromQti(item).asOpt[PlayerDefinition]

      require(newDef.isDefined, "There must be a player definition created")

      newDef.map { d => itemService.save(item.withPlayerDefinition(d)) }
      newDef.get
    }
  }

  def updateV2Json(item: Item): Item = {
    item.createdByApiVersion match {
      case 1 => {
        logger.debug(s"itemId=${item.id} function=updateV2Json#Item")
        transformToV2Json(item, Some(createFromQti(item))).asOpt[PlayerDefinition]
          .map(playerDefinition => item.withPlayerDefinition(playerDefinition)) match {
            case Some(updatedItem) => item.playerDefinition.equals(updatedItem.playerDefinition) match {
              case true => updatedItem
              case _ => {
                logger.trace(s"itemId=${item.id} function=updateV2Json#Item - saving item")
                itemService.save(updatedItem)
                updatedItem
              }
            }
            case _ => item
          }
      }
      case _ => item
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
        if (checkModelIsUpToDate && item.createdByApiVersion == 1) {
          val rawMappedThroughPlayerDef = createFromQti(item).asOpt[PlayerDefinition].map(Json.toJson(_))
          for {
            rawPd <- rawMappedThroughPlayerDef
            itemPd <- itemPlayerDef
          } yield {
            compareJson(item.id, "createdFromQti-vs-item.playerDefinition", rawPd, itemPd)
          }
        }
        itemPlayerDef match {
          case Some(itemPlayer) => itemPlayer
          case None if item.createdByApiVersion == 1 => createFromQti(item)
          case _ => throw new IllegalArgumentException(s"Item ${item.id} did not contain QTI XML or component JSON, ${item.createdByApiVersion}")
        }
      }
    })
    val profile = toProfile(item)

    val collectionJs = (for {
      collectionId <- item.collectionId
      collection <- findCollection(new ObjectId(collectionId))
    } yield Json.toJson(collection)).getOrElse(Json.obj())

    val out = root ++ Json.obj(
      "itemId" -> Json.toJson(item.id.toString()),
      "profile" -> profile,
      "supportingMaterials" -> Json.toJson(item.supportingMaterials),
      "collection" -> collectionJs)

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

  // Is this meant to return Unit?!
  private def compareJson(itemId: VersionedId[ObjectId], msg: String, a: JsValue, b: JsValue) = {
    JsonCompare.caseInsensitiveSubTree(a, b) match {
      case Left(diffs) => {
        diffs.foreach { d =>
          logger.warn(s"itemId=${itemId} msg=$msg function=compareJson diff=$d")
        }
        logger.trace(s"itemId=$itemId a=${Json.prettyPrint(a)}")
        logger.trace(s"itemId=$itemId b=${Json.prettyPrint(b)}")
      }
      case Right(_) => logger.debug(s"itemId=${itemId} msg=$msg function=compareJson - json is identical")
    }
  }

  private def getTransformation(item: Item): JsValue = {
    logger.debug(s"itemId=${item.id} function=getTransformation - generating json")
    val qti = for {
      data <- item.data
      qti <- data.files.find(_.name == "qti.xml")
    } yield qti.asInstanceOf[VirtualFile]
    require(qti.isDefined, s"item: ${item.id} has no qti xml. data: ${item.data}")
    val transformedJson = QtiTransformer.transform(scala.xml.XML.loadString(XMLCleaner.clean(CDataHandler.addCDataTags(qti.get.content))))
    logger.trace(s"itemId=${item.id} function=getTransformation generatedJson=${Json.stringify(transformedJson)}")
    transformedJson
  }

}
