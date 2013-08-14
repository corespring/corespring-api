package org.corespring.platform.core.models.item

import org.corespring.platform.core.models.json.JsonValidationException
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.collection.mutable.Map
import com.mongodb.casbah.Imports._
import play.api.libs.json.JsArray
import play.api.libs.json.JsUndefined
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsString
import scala.Some
import play.api.libs.json.JsObject
import play.api.data.validation.ValidationError

case class TaskInfo( var extended: Seq[Metadata] = Seq(),
                     subjects: Option[Subjects] = None,
                     gradeLevel: Seq[String] = Seq(),
                     title: Option[String] = None,
                     itemType: Option[String] = None){
  def cloneInfo(titlePrefix:String) : TaskInfo = {
    require(titlePrefix != null)
    copy( title = title.map( t => if(t.isEmpty) titlePrefix else titlePrefix + " " + t) orElse Some(titlePrefix))
  }
}
object TaskInfo extends ValueGetter {


  object Keys {
    val title = "title"
    val gradeLevel = "gradeLevel"
    val itemType = "itemType"
    val subjects = "subjects"
    val extended = "extended"
  }

  implicit object Writes extends Writes[TaskInfo] {
    def writes(info:TaskInfo) : JsValue = {

      import Keys._

      val infoJson = JsObject(Seq(
          if (info.gradeLevel.isEmpty) None else Some((gradeLevel -> JsArray(info.gradeLevel.map(JsString(_))))),
          info.title.map((title -> JsString(_))),
          info.itemType.map((itemType -> JsString(_)))
        ).flatten)

      val subjectsJson : Option[JsValue] = info.subjects.map( subjects => Json.toJson(subjects))

      subjectsJson match {
        case Some(js) => {
          val jsObjects = Seq(infoJson, js).filter(_.isInstanceOf[JsObject]).map(_.asInstanceOf[JsObject])
          jsObjects.tail.foldRight(jsObjects.head)(_ ++ _)
        }
        case _ => infoJson
      }
    }
  }
  def extendedAsJson(extended: Seq[Metadata]):JsValue = {
    JsObject(extended.foldRight[Seq[(String,JsValue)]](Seq())((md,acc1) => {
      acc1 :+ (md.metadataKey -> JsObject(md.props.toSeq.map(prop => prop._1 -> JsString(prop._2))))
    }))
  }
  private def isValid(g:String) = fieldValues.gradeLevels.exists(_.key == g)
  private val getGradeLevel = Reads[Seq[String]]((json:JsValue) => {
    (json \ Keys.gradeLevel).asOpt[Seq[String]] match {
      case Some(grades) => if (grades.forall(isValid(_))) JsSuccess(grades)
        else JsError(__ \ Keys.gradeLevel, ValidationError("missing", Keys.gradeLevel))
      case None => JsSuccess(Seq())
    }
  })
  private val getExtended = Reads[Seq[Metadata]]((json:JsValue) => {
    (json \ Keys.extended) match {
      case JsObject(metadatas) => {
        metadatas.foldRight[Either[JsError,Seq[Metadata]]](Right(Seq[Metadata]()))((jsmetadata,acc) => {
          val (metadataKey, jsprops) = jsmetadata
          val optprops:Either[JsError,Map[String,String]] = (jsprops match {
            case JsObject(fields) => Right(fields.foldRight[Map[String,String]](Map())((field,acc) => {
              acc + (field._1 -> field._2.toString())
            }))
            case JsUndefined(_) => Right(Map[String,String]())
            case _ => Left(JsError(__ \ metadataKey, ValidationError("incorrect format","props must be a JSON object")))
          })
          optprops match {
            case Right(props) => acc.fold(error => Left(error),mds => Right(mds :+ Metadata(metadataKey,props)))
            case Left(e) => acc.fold(error => Left(JsError(e.errors ++ error.errors)),_ => Left(e))
          }
        }) match {
          case Right(props) => JsSuccess(props)
          case Left(jserror) => jserror
        }
      }
      case JsUndefined(_) => JsSuccess(Seq())
      case _ => JsError(__ \ Keys.extended, ValidationError("incorrect format","json for extended property was not a JSON object"))
    }
  })
  private val getSubjects = Reads[Option[Subjects]]((json:JsValue) =>
    Json.fromJson[Subjects](json).fold(_ => JsSuccess(None), valid => JsSuccess(Some(valid)))
  )
  implicit val taskInfoReads:Reads[TaskInfo] = (
    getExtended and
    getSubjects and
    getGradeLevel and
    (__ \ Keys.title).readNullable[String] and
    (__ \ Keys.itemType).readNullable[String]
  )(TaskInfo.apply _)
}

case class Metadata(metadataKey: String, props: Map[String,String])


