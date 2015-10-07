package org.corespring.it.helpers

import bootstrap.Main
import org.bson.types.ObjectId
import org.corespring.models.item.{ FieldValue, ListKeyValue, StringKeyValue }

object FieldValuesHelper {

  val fieldValueService = Main.fieldValueService
  val dummy = StringKeyValue("dummy", "dummy")
  val dummyList = ListKeyValue("dummy", Seq.empty)
  val fieldValue = FieldValue(
    gradeLevels = Seq(dummy),
    reviewsPassed = Seq(dummy),
    mediaType = Seq(dummy),
    keySkills = Seq(dummyList),
    itemTypes = Seq(dummyList),
    licenseTypes = Seq(dummy),
    priorUses = Seq(dummy),
    depthOfKnowledge = Seq(dummy),
    credentials = Seq(dummy),
    bloomsTaxonomy = Seq(dummy))

  def init():Option[ObjectId] = {
    fieldValueService.insert(fieldValue).toOption
  }

  def cleanup(id:Option[ObjectId]) = {
    fieldValueService.delete(id.get)
  }
}
