package models.item

import controllers.JsonValidationException
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.collection.mutable.Map

case class TaskInfo( extended: Map[String,Map[String,String]] = Map(),
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
  def extendedAsJson(extended: Map[String,Map[String,String]]):JsValue = {
    JsObject(extended.foldRight[Seq[(String,JsValue)]](Seq())((set,acc1) => {
      acc1 :+ (set._1 -> JsObject(set._2.foldRight[Seq[(String,JsValue)]](Seq())((prop,acc2) => acc2 :+ (prop._1 -> JsString(prop._2)))))
    }))
  }
  implicit object Reads extends Reads[TaskInfo] {
    def reads(json:JsValue) : JsResult[TaskInfo] = {

      import Keys._
      import play.api.data.validation.ValidationError

      def isValid(g:String) = fieldValues.gradeLevels.exists(_.key == g)

      /** Look up the grade level string in fieldValues to ensure its valid
       * @return a sequence of gradelevel strings of throw a JsonValidationException
       */
      def getGradeLevel:JsResult[Seq[String]] = (json \ gradeLevel).asOpt[Seq[String]].map { v =>
          if (v.foldRight[Boolean](true)((g, acc) => isValid(g) && acc)) JsSuccess(v)
          else JsError(__ \ gradeLevel, ValidationError("missing", gradeLevel))
      }.getOrElse(JsSuccess(Seq()))

      def getExtended:JsResult[Map[String,Map[String,String]]] = {
        (json \ "extended") match {
          case JsObject(fields1) => {
            fields1.foldRight[Either[JsError,Map[String,Map[String,String]]]](Right(Map()))((prop,acc) => {
              prop._2 match {
                case JsObject(innerFields) => {
                  innerFields.foldRight[Either[JsError,Map[String,String]]](Right(Map()))((innerProp,innerAcc) => {
                    innerProp._2.validate[String] match {
                      case JsSuccess(value,_) => innerAcc.fold(Left(_), innerProps => Right(innerProps + (innerProp._1 -> value)))
                      case JsError(errors) => innerAcc.fold(jserror => Left(JsError(jserror.errors ++ errors)), _ => Left(JsError(errors)))
                    }
                  }) match {
                    case Right(innerProps) => acc.fold(Left(_),props => Right(props + (prop._1 -> innerProps)))
                    case Left(innerJserror) => acc match {
                      case Right(_) => Left(innerJserror)
                      case Left(jserror) => Left(JsError(jserror.errors ++ innerJserror.errors))
                    }
                  }
                }
                case _ => Left(JsError(__ \ prop._1, ValidationError("incorrect format", "json object not found for "+prop._1+"property")))
              }
            }) match {
              case Right(props) => JsSuccess(props)
              case Left(jserror) => jserror
            }
          }
          case JsUndefined(_) => JsSuccess(Map())
          case _ => JsError(__ \ extended, ValidationError("incorrect format","json for extended property was not a JSON object"))
        }
      }

      getGradeLevel.flatMap(gradeLevel => getExtended.map(extended =>
        TaskInfo(
          extended = extended,
          subjects = json.asOpt[Subjects],
          gradeLevel = gradeLevel,
          title = (json \ title).asOpt[String],
          itemType = (json \ itemType).asOpt[String]
        )
      ))
    }
  }
}


