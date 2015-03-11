package org.corespring.platform.core.services.item

import com.mongodb.casbah.Imports
import org.corespring.platform.core.models.item.{PlayerDefinition, Item}
import org.corespring.platform.core.models.itemSession.ItemSessionCompanion
import org.corespring.platform.data.mongo.SalatVersioningDao
import org.corespring.test.utils.mocks.MockS3Service
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.libs.json.Json

class ItemServiceWiredTest extends Specification with Mockito {

  val mockDao: SalatVersioningDao[Item] = mock[SalatVersioningDao[Item]]

  val itemService = new ItemServiceWired(new MockS3Service(), mock[ItemSessionCompanion], mockDao)

  "save" should {

    val itemTypes = Map("corespring-multiple-choice" -> 1, "corespring-text-entry" -> 2)

    val components = itemTypes.map{ case(itemType, count) => List.fill(count)(Json.obj("componentType" -> itemType)) }
      .flatten.zipWithIndex.map{ case (obj, index) => Json.obj(index.toString -> obj) }
      .foldLeft(Json.obj()){ (obj, acc) => acc ++ obj }

    val playerDefinition = new PlayerDefinition(files = Seq.empty, xhtml = "",
      components = components, summaryFeedback = "", customScoring = None)

    val item = Item(playerDefinition = Some(playerDefinition))

    "add itemTypes to taskInfo" in {
      val captor = capture[Item]
      itemService.save(item)
      there was one(mockDao).save(captor, any[Boolean])

      val transformedItem = captor.value

      transformedItem.taskInfo.map(_.itemTypes) must beEqualTo(Some(itemTypes))
    }

  }

}
