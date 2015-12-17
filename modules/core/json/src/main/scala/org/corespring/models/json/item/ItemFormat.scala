package org.corespring.models.json.item

import org.bson.types.ObjectId
import org.corespring.models.item.Item.Keys
import org.corespring.models.item._
import org.corespring.models.item.resource.Resource
import org.corespring.models.json.{ ObjectIdFormat, JsonValidationException, ValueGetter, VersionedIdFormat }
import org.corespring.models.{ Standard, item => model }
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json._

trait ItemFormat extends Format[model.Item] with ValueGetter {

  implicit def cd: Format[ContributorDetails]

  implicit def ac: Format[AdditionalCopyright]

  implicit def pd: Format[PlayerDefinition]

  implicit def ti: Format[TaskInfo]

  implicit def a: Format[Alignments]

  implicit def wf: Format[Workflow]

  implicit def res: Format[Resource]

  implicit def st: Format[Standard]

  def findOneByDotNotation(dotNotation: String): Option[Standard]

  def writes(item: model.Item) = {

    def toJsObject[T](a: Option[T])(implicit w: Writes[T]): Option[JsObject] = a.map(w.writes(_).asInstanceOf[JsObject])

    val mainItem: JsObject = writeMainItem(item)
    val details: Option[JsObject] = toJsObject(item.contributorDetails)
    val taskInfo: Option[JsObject] = toJsObject(item.taskInfo)
    val alignments: Option[JsObject] = toJsObject(item.otherAlignments)

    val out = Seq(Some(mainItem), details, taskInfo, alignments).flatten
    out.tail.foldRight(out.head)(_ ++ _)
  }

  def writeMainItem(item: Item): JsObject = {

    implicit val vif = VersionedIdFormat

    val basics: Seq[Option[(String, JsValue)]] = Seq(
      Some(("id" -> Json.toJson(item.id))),
      item.clonedFromId.map( c => "clonedFromId" -> Json.toJson(c)),
      Some(Keys.collectionId -> JsString(item.collectionId)),
      item.workflow.map((Keys.workflow -> Json.toJson(_))),
      item.data.map((Keys.data -> Json.toJson(_))),
      item.playerDefinition.map("playerDefinition" -> Json.toJson(_)),
      Some(Keys.contentType -> JsString(Item.contentType)),
      Some(Keys.published -> JsBoolean(item.published)))

    def makeJsString(tuple: (String, Option[String])) = {
      val (key, value) = tuple
      value match {
        case Some(s) => Some((key, JsString(s)))
        case _ => None
      }
    }

    val strings: Seq[Option[(String, JsValue)]] = Seq(
      (Keys.lexile, item.lexile),
      (Keys.pValue, item.pValue),
      (Keys.priorUse, item.priorUse)).map(makeJsString)

    def makeJsArray(tuple: (String, Seq[JsValue])) = {
      val (key, value) = tuple
      if (value.isEmpty)
        None
      else
        Some(key, JsArray(value))
    }

    val validStandards: Seq[Standard] = item.standards.map(findOneByDotNotation).flatten

    val arrays: Seq[Option[(String, JsValue)]] = Seq(
      (Keys.priorGradeLevel, item.priorGradeLevels.map(JsString(_))),
      (Keys.reviewsPassed, item.reviewsPassed.map(JsString(_))),
      (Keys.supportingMaterials, item.supportingMaterials.map(Json.toJson(_))),
      (Keys.standards, validStandards.map(Json.toJson(_)))).map(makeJsArray)

    val joined = (basics ++ strings ++ arrays).flatten
    JsObject(joined)
  }

  def reads(json: JsValue) = {

    implicit val oidf = ObjectIdFormat

    def areAllGradeLevelsValid(gradeLevels: Seq[String]): Boolean = {
      println(s"gradeLevels: $gradeLevels")
      println(s"fv.gradeLevels: ${fieldValues.gradeLevels.map(_.key)}")

      val invalid = gradeLevels.exists(gl => !fieldValues.gradeLevels.map(_.key).contains(gl))
      !invalid
    }

    val item = Item(
      collectionId = (json \ Keys.collectionId).as[String],
      playerDefinition = (json \ "playerDefinition").asOpt[model.PlayerDefinition],
      taskInfo = json.asOpt[model.TaskInfo],
      otherAlignments = json.asOpt[model.Alignments],
      workflow = (json \ Keys.workflow).asOpt[model.Workflow],
      contributorDetails = json.asOpt[model.ContributorDetails],
      lexile = (json \ Keys.lexile).asOpt[String],
      pValue = (json \ Keys.pValue).asOpt[String],
      supportingMaterials = (json \ Keys.supportingMaterials).asOpt[Seq[Resource]].getOrElse(Seq()),
      priorUse = (json \ Keys.priorUse).asOpt[String],
      priorUseOther = (json \ Keys.priorUseOther).asOpt[String],
      priorGradeLevels = {
        (json \ Keys.priorGradeLevel).asOpt[Seq[String]].map { levels =>
          if (areAllGradeLevelsValid(levels)) {
            levels
          } else {
            throw new JsonValidationException(s"${Keys.priorGradeLevel} -> $levels")
          }
        }.getOrElse(Seq.empty)
      },
      reviewsPassed = (json \ Keys.reviewsPassed).asOpt[Seq[String]].getOrElse(Seq.empty),
      reviewsPassedOther = (json \ Keys.reviewsPassedOther).asOpt[String],
      sharedInCollections = (json \ Keys.sharedInCollections).asOpt[Seq[ObjectId]].getOrElse(Seq.empty),
      standards = (json \ Keys.standards).asOpt[Seq[String]].getOrElse(Seq()),
      data = (json \ Keys.data).asOpt[Resource],
      published = (json \ Keys.published).asOpt[Boolean].getOrElse(false))

    try {
      val withId = item.copy(id =
        (json \ Keys.id).asOpt[VersionedId[ObjectId]](VersionedIdFormat)
          .getOrElse(VersionedId(new ObjectId())))
      JsSuccess(withId)
    } catch {
      case e: Throwable => throw new JsonValidationException(s"Bad id ${(json \ Keys.id).asOpt[String]}")
    }
  }
}
