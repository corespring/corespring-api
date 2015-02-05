package org.corespring.platform.core.models.item

import org.corespring.platform.core.models.json.JsonValidationException
import play.api.libs.json._
import org.corespring.test.BaseTest

class TaskInfoTest extends BaseTest {

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
      json.as[TaskInfo] must throwA[JsResultException]
    }

    "parse itemType" in {
      val taskInfo = TaskInfo(itemType = Some("itemType"))
      val json = Json.toJson(taskInfo)
      val parsed = json.as[TaskInfo]
      parsed.itemType must equalTo(taskInfo.itemType)
    }

  }

  "taskInfoReads" should {

    import TaskInfo._

    val itemTypes = Map("corespring-text-entry" -> 5, "corespring-multiple-choice" -> 4)
    val json = Json.obj("itemTypes" -> itemTypes)

    "deserialize itemTypes" in {
      Json.fromJson[TaskInfo](json)
        .getOrElse(throw new Exception(s"Could not read ${Json.prettyPrint(json)} as TaskInfo"))
        .itemTypes must beEqualTo(itemTypes)
    }
  }

}
