package org.corespring.api.v1

import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.platform.core.models.item.resource.{VirtualFile, BaseFile}
import org.corespring.test.helpers.models._
import org.corespring.v2.player.scopes
import play.api.libs.json.{JsString, Json, JsValue}
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.platform.core.models.item.Item

trait itemData extends scopes.orgWithAccessToken {

  val collectionId = CollectionHelper.create(orgId)
  val itemId = ItemHelper.create(collectionId)

  val routes = org.corespring.api.v1.routes.ItemApi
  val sessionRoutes = org.corespring.api.v1.routes.ItemSessionApi

  def item(id: VersionedId[ObjectId]) = ItemHelper.get(id)

  def jsonRequest(url:String, method:String, body: JsValue = JsString("")) = {
    val fakeRequest = FakeRequest(method, url, FakeHeaders(), body)
    val Some(result) = route(fakeRequest)
    contentAsJson(result)
  }

  def amendItemDataForVersion(item:Item, version: String):Item = {
    def amendFiles(files:Seq[BaseFile]):Seq[BaseFile] = {
      files.foreach(f=>f match {
        case vf:VirtualFile => vf.content = ItemHelper.qtiXmlTemplate.replaceAll("::version::", version)
      })
      files
    }
    val ti = item.taskInfo match {
      case Some(taskInfo) => Some(taskInfo.copy(title = Some("Title " + version)))
      case _ => item.taskInfo
    }
    val itemData = item.data match {
      case Some(data) => Some(data.copy(files = amendFiles(data.files)))
      case _ => item.data
    }
    item.copy(taskInfo = ti, data = itemData)
  }

  def triggerVersionBump(id: VersionedId[ObjectId], version: String):JsValue = {
    val unversionedId = id.copy[ObjectId](version = None)
    val theItem = ItemHelper.get(unversionedId).get

    // create session
    jsonRequest(s"${sessionRoutes.create(theItem.id).url}?access_token=$accessToken", POST)

    // update item
    val newJson = Json.toJson(amendItemDataForVersion(item(theItem.id).get, version))
    jsonRequest(s"${routes.update(theItem.id).url}?access_token=$accessToken", PUT, newJson)
  }

  override def before: Any = {
    super.before
  }

  override def after: Any = {
    super.after
    CollectionHelper.delete(collectionId)
    ItemHelper.delete(itemId)
  }

}

class ItemVersioningIntegrationTest extends IntegrationSpecification {

  "item versioning scenario" in new itemData {
    // We publish the original item
    ItemHelper.publish(itemId)

    // We create a session and save the item to trigger 10 version bumps
    for (i <- 1 to 10) {
      triggerVersionBump(itemId, i.toString)
    }

    def titleForItem(item:Item) = item.taskInfo match {
      case Some(ti) => ti.title.getOrElse("")
      case _ => ""
    }

    def qtiForItem(item:Item) = item.data match {
      case Some(data) => data.files.find(_.name == "qti.xml") match {
        case Some(f:VirtualFile) => f.content
        case _ => ""
      }
      case _ => ""
    }

    // Version 0 of the item
    val initialItem = ItemHelper.get(itemId.copy(version = Some(0))).get
    initialItem.taskInfo.get.title.get === "Title"
    qtiForItem(initialItem) must contain("::version::")

    for (i <- 1 to 10) {
      val item = ItemHelper.get(itemId.copy(version = Some(i))).get
      item.taskInfo.get.title.get must be equalTo "Title "+i
      qtiForItem(item) must contain(s"<itemBody>${i}</itemBody>")
    }

  }

}
