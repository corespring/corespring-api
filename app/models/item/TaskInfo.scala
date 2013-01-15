package models.item

import models.item._
import play.api.libs.json._
import org.bson.types.ObjectId
import controllers.JsonValidationException

case class TaskInfo(
                     subjects: Option[Subjects] = None,
                     gradeLevel: Seq[String] = Seq(),
                     title: Option[String] = None,
                     itemType: Option[String] = None)

object TaskInfo extends ValueGetter {

  object Keys {
    val title = "title"
    val gradeLevel = "gradeLevel"
    val itemType = "itemType"
  }

  def json(info: TaskInfo): Seq[(String, JsValue)] = {
    import Keys._

    val seq: Seq[Option[(String, JsValue)]] =
      Seq(
        if (info.gradeLevel.isEmpty) None else Some((gradeLevel -> JsArray(info.gradeLevel.map(JsString(_))))),
        info.title.map((title -> JsString(_))),
        info.itemType.map((itemType -> JsString(_)))
      )

    val subjectsSeq = info.subjects.map(subjects => Subjects.json(subjects))
    seq.flatten ++ subjectsSeq.getOrElse(Seq())
  }

  def obj(json: JsValue): Option[TaskInfo] = {

    import Keys._

    def getGradeLevel = (json \ gradeLevel).asOpt[Seq[String]].map {
      v =>
        if (v.foldRight[Boolean](true)((g, acc) => fieldValues.gradeLevels.exists(_.key == g) && acc))
          v
        else
          throw new JsonValidationException(gradeLevel)
    }


    Some(TaskInfo(
      subjects = Subjects.obj(json),
      gradeLevel = getGradeLevel.getOrElse(Seq()),
      title = (json \ title).asOpt[String],
      itemType = (json \ itemType).asOpt[String]
    ))
  }



}


