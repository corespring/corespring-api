package models.item

import controllers.JsonValidationException
import play.api.libs.json._

case class TaskInfo(
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

  implicit object Reads extends Reads[TaskInfo] {
    def reads(json:JsValue) : TaskInfo = {

      import Keys._

      def isValid(g:String) = fieldValues.gradeLevels.exists(_.key == g)

      /** Look up the grade level string in fieldValues to ensure its valid
       * @return a sequence of gradelevel strings of throw a JsonValidationException
       */
      def getGradeLevel = (json \ gradeLevel).asOpt[Seq[String]].map {
        v =>
          if (v.foldRight[Boolean](true)((g, acc) => isValid(g) && acc))
            v
          else
            throw new JsonValidationException(gradeLevel)
      }

      TaskInfo(
        subjects = json.asOpt[Subjects],
        gradeLevel = getGradeLevel.getOrElse(Seq()),
        title = (json \ title).asOpt[String],
        itemType = (json \ itemType).asOpt[String]
      )

    }
  }
}


