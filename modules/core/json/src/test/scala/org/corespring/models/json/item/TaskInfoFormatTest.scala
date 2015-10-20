package org.corespring.models.json.item

import org.bson.types.ObjectId
import org.corespring.models.Subject
import org.corespring.models.item.{ StringKeyValue, Subjects, FieldValue, TaskInfo }
import org.corespring.models.json.{ SubjectWrites, SubjectsFormat }
import org.specs2.mutable.Specification
import play.api.libs.json.{ Writes, Format, Json }

class TaskInfoFormatTest extends Specification {

  val fv = FieldValue(gradeLevels = Seq(
    StringKeyValue("03", "03"),
    StringKeyValue("04", "04")
  ))

  implicit val tif: Format[TaskInfo] = new TaskInfoFormat {
    override implicit def sf: Format[Subjects] = new SubjectsFormat {
      override def findOneById(id: ObjectId): Option[Subject] = Some(Subject("?"))

      override implicit def sf: Writes[Subject] = SubjectWrites

      override def fieldValues: FieldValue = fv
    }

    override def fieldValues: FieldValue = fv
  }

  "TaskInfo" should {

    "parses gradeLevel" in {
      val taskInfo = TaskInfo(gradeLevel = Seq("03", "04"))
      val json = Json.toJson(taskInfo)
      val parsedItem = json.as[TaskInfo]
      parsedItem.gradeLevel must equalTo(taskInfo.gradeLevel)
    }

    "does not parse invalid gradeLevel" in {
      val taskInfo = TaskInfo(gradeLevel = Seq("apple", "pear"))
      val json = Json.toJson(taskInfo)
      json.as[TaskInfo].gradeLevel must_== Seq.empty
    }

    "parse itemType" in {
      val taskInfo = TaskInfo(itemType = Some("itemType"))
      val json = Json.toJson(taskInfo)
      val parsed = json.as[TaskInfo]
      parsed.itemType must equalTo(taskInfo.itemType)
    }

  }

}
