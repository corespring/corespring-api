package tests.models

import tests.BaseTest
import play.api.mvc.{AnyContentAsJson, Call}
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.libs.json._
import play.api.test.Helpers._
import org.bson.types.ObjectId
import com.mongodb.casbah.Imports._
import com.novus.salat._
import play.api.mvc.Call
import play.api.test.FakeHeaders
import play.api.mvc.AnyContentAsJson
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsBoolean
import models.item.{Item, Version}
import models.mongoContext._
import models.item.resource.VirtualFile


class VersioningTest extends BaseTest{

  "saving an item resource with no sessions returns successfully" in {
    pending
    true === true
  }
  "saving an item resource with changes and sessions but with a force query field returns successfully" in {
    pending
    true === true
  }

  "saving a published item resource with changes and sessions and with no force query field returns a forbidden message" in {
    pending
    true === true
  }

  "saving a draft item resource with changes and sessions and with no force query field returns successfully" in {
    pending
    true === true
  }

  "incrementing an item and then querying for a list of items returns the new revision of that item and does not include the old" in {
    pending
    true === true
  }
}
