package org.corespring.platform.core.models.item

import com.mongodb.casbah.Imports._
import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsUndefined
import play.api.libs.json._
import scala.Some
import scala.collection.mutable.Map

case class TaskInfo(var extended: Map[String, BasicDBObject] = Map(),
  subjects: Option[Subjects] = None,
  gradeLevel: Seq[String] = Seq(),
  title: Option[String] = None,
  description: Option[String] = None,
  itemType: Option[String] = None) {
  def cloneInfo(titlePrefix: String): TaskInfo = {
    require(titlePrefix != null)
    copy(title = title.map(t => if (t.isEmpty) titlePrefix else titlePrefix + " " + t) orElse Some(titlePrefix))
  }
}
object TaskInfo extends ValueGetter {

  object Keys {
    val title = "title"
    val description = "description"
    val gradeLevel = "gradeLevel"
    val itemType = "itemType"
    val subjects = "subjects"
    val extended = "extended"
  }

  val standardsSorter: (String, String) => Boolean = (a,b) => a <= b

  val gradeLevelSorter: (String, String) => Boolean = (a,b) => {
    val reference = List("PK", "KG", "01", "02", "03", "04", "05", "06", "07", "08", "09",
      "10", "11", "12", "13", "PS", "AP", "UG")
    (Option(reference.indexOf(a)), Option(reference.indexOf(b))) match {
      case (Some(one), Some(two)) => (one <= two)
      case _ => a <= b
    }
  }

  implicit object Writes extends Writes[TaskInfo] {
    def writes(info: TaskInfo): JsValue = {

      import Keys._

      val infoJson = JsObject(Seq(
        if (info.gradeLevel.isEmpty) None else Some((gradeLevel -> JsArray(info.gradeLevel.map(JsString(_))))),
        info.title.map((title -> JsString(_))),
        info.description.map((description -> JsString(_))),
        info.itemType.map((itemType -> JsString(_))),
        if (info.extended.isEmpty) None else Some((extended -> extendedAsJson(info.extended)))).flatten)

      val subjectsJson: Option[JsValue] = info.subjects.map(subjects => Json.toJson(subjects))

      subjectsJson match {
        case Some(js) => {
          val jsObjects = Seq(infoJson, js).filter(_.isInstanceOf[JsObject]).map(_.asInstanceOf[JsObject])
          jsObjects.tail.foldRight(jsObjects.head)(_ ++ _)
        }
        case _ => infoJson
      }
    }
  }
  def extendedAsJson(extended: Map[String, BasicDBObject]): JsValue = {
    JsObject(extended.foldRight[Seq[(String, JsValue)]](Seq())((md, acc1) => {
      acc1 :+ (md._1 -> JsObject(md._2.toSeq.map(prop => prop._1 -> JsString(prop._2.toString))))
    }))
  }
  private def isValid(g: String) = fieldValues.gradeLevels.exists(_.key == g)
  private val getGradeLevel = Reads[Seq[String]]((json: JsValue) => {
    (json \ Keys.gradeLevel).asOpt[Seq[String]] match {
      case Some(grades) => if (grades.forall(isValid(_))) JsSuccess(grades)
      else JsError(__ \ Keys.gradeLevel, ValidationError("missing", Keys.gradeLevel))
      case None => JsSuccess(Seq())
    }
  })
  private val getExtended = Reads[Map[String, BasicDBObject]]((json: JsValue) => {
    (json \ Keys.extended) match {
      case JsObject(metadatas) => {
        metadatas.foldRight[Either[JsError, Map[String, BasicDBObject]]](Right(Map[String, BasicDBObject]()))((jsmetadata, acc) => {
          val (metadataKey, jsprops) = jsmetadata
          val optprops: Either[JsError, Map[String, String]] = (jsprops match {
            case JsObject(fields) => Right(fields.foldRight[Map[String, String]](Map())((field, acc) => {
              acc + (field._1 -> field._2.toString())
            }))
            case JsUndefined() => Right(Map[String, String]())
            case _ => Left(JsError(__ \ metadataKey, ValidationError("incorrect format", "props must be a JSON object")))
          })
          import collection.JavaConversions._
          optprops match {
            case Right(props) => acc.fold(error => Left(error), mds => Right(mds + (metadataKey -> new BasicDBObject(props))))
            case Left(e) => acc.fold(error => Left(JsError(e.errors ++ error.errors)), _ => Left(e))
          }
        }) match {
          case Right(props) => JsSuccess(props)
          case Left(jserror) => jserror
        }
      }
      case JsUndefined() => JsSuccess(Map())
      case _ => JsError(__ \ Keys.extended, ValidationError("incorrect format", "json for extended property was not a JSON object"))
    }
  })
  private val getSubjects = Reads[Option[Subjects]]((json: JsValue) =>
    Json.fromJson[Subjects](json).fold(_ => JsSuccess(None), valid => JsSuccess(Some(valid))))
  implicit val taskInfoReads: Reads[TaskInfo] = (
    getExtended and
    getSubjects and
    getGradeLevel and
    (__ \ Keys.title).readNullable[String] and
    (__ \ Keys.description).readNullable[String] and
    (__ \ Keys.itemType).readNullable[String])(TaskInfo.apply _)
}

