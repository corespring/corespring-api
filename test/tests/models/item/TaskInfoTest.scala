package tests.models.item

import org.specs2.mutable.Specification
import models.item.TaskInfo
import play.api.libs.json._
import controllers.JsonValidationException
import tests.PlaySingleton

class TaskInfoTest extends Specification {

  PlaySingleton.start()

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
      json.as[TaskInfo] must throwA[JsonValidationException]
    }

    "parse itemType" in {

      val taskInfo = TaskInfo(itemType = Some("itemType"))
      val json = Json.toJson(taskInfo)
      val parsed = json.as[TaskInfo]
      parsed.itemType must equalTo(taskInfo.itemType)
    }

  }

}
