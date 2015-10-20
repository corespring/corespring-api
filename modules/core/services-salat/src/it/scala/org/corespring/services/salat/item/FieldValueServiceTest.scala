package org.corespring.services.salat.item

import org.corespring.models.item._
import org.corespring.services.salat.ServicesSalatIntegrationTest
import org.specs2.mock.Mockito
import org.specs2.specification.Scope

import scalaz.Success

class FieldValueServiceTest extends ServicesSalatIntegrationTest with Mockito {

  lazy val fieldValueService = services.fieldValueService

  private def mkStringKeyValueSequence(id: String) = {
    Seq.tabulate(3) {
      i => StringKeyValue(id + i, id + i)
    }
  }

  private def mkListKeyValueSequence(id: String) = {
    Seq.tabulate(3) {
      i =>
        ListKeyValue(id + i,
          Seq.tabulate(3) {
            j => id + j
          })
    }
  }

  private def mkFieldValue() = {
    FieldValue(
      version = Some("1.2.3"),
      gradeLevels = mkStringKeyValueSequence("gl"),
      reviewsPassed = mkStringKeyValueSequence("rp"),
      mediaType = mkStringKeyValueSequence("mt"),
      keySkills = mkListKeyValueSequence("ks"),
      itemTypes = mkListKeyValueSequence("it"),
      licenseTypes = mkStringKeyValueSequence("lt"),
      priorUses = mkStringKeyValueSequence("pu"),
      depthOfKnowledge = mkStringKeyValueSequence("dk"),
      credentials = mkStringKeyValueSequence("cr"),
      bloomsTaxonomy = mkStringKeyValueSequence("bt"))
  }

  "get" should {
    "return a fieldValue previously inserted" in new Scope {
      val fv = mkFieldValue()
      fieldValueService.insert(fv).getOrElse(failure("insert failed"))
      val res = fieldValueService.get
      res !== None
      val fvDb = res.get
      fvDb.gradeLevels === fv.gradeLevels
      fvDb.reviewsPassed === fv.reviewsPassed
      fvDb.mediaType === fv.mediaType
      fvDb.keySkills === fv.keySkills
      fvDb.itemTypes === fv.itemTypes
      fvDb.licenseTypes === fv.licenseTypes
      fvDb.priorUses === fv.priorUses
      fvDb.depthOfKnowledge === fv.depthOfKnowledge
      fvDb.credentials === fv.credentials
      fvDb.bloomsTaxonomy === fv.bloomsTaxonomy
      fvDb === fv //to make sure we didn't miss something
    }

    "return the last fieldValue inserted" in new Scope {
      val fv1 = mkFieldValue()
      fieldValueService.insert(fv1).getOrElse(failure("insert failed"))
      val fv2 = mkFieldValue()
      fieldValueService.insert(fv2).getOrElse(failure("insert failed"))
      val fv3 = mkFieldValue()
      fieldValueService.insert(fv3).getOrElse(failure("insert failed"))

      val fvDb = fieldValueService.get.get
      fvDb.id === fv3.id
    }
  }

  "insert" should {
    "insert data into db" in new Scope {
      val fv = mkFieldValue()
      fieldValueService.insert(fv) must_== Success(fv.id)
    }
  }

  "delete" should {
    "work" in new Scope {
      val fv = mkFieldValue()
      fieldValueService.insert(fv).getOrElse(failure("insert failed"))
      fieldValueService.delete(fv.id) must_== Success(fv.id)
    }
  }

}
