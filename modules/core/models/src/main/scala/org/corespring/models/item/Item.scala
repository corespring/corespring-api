package org.corespring.models.item

import org.bson.types.ObjectId
import org.corespring.models.item.resource.Resource
import org.corespring.platform.data.mongo.models.{ EntityWithVersionedId, VersionedId }
import org.joda.time.DateTime

case class ItemStandards(title: String, standards: Seq[String], id: VersionedId[ObjectId])

case class Item(
  collectionId: String,
  clonedFromId: Option[VersionedId[ObjectId]] = None,
  contentType: String = Item.contentType,
  contributorDetails: Option[ContributorDetails] = None,
  data: Option[Resource] = None,
  dateModified: Option[DateTime] = Some(new DateTime()),
  id: VersionedId[ObjectId] = VersionedId(ObjectId.get(), Some(0)),
  lexile: Option[String] = None,
  otherAlignments: Option[Alignments] = None,
  playerDefinition: Option[PlayerDefinition] = None,
  priorGradeLevels: Seq[String] = Seq(),
  priorUse: Option[String] = None,
  priorUseOther: Option[String] = None,
  published: Boolean = false,
  pValue: Option[String] = None,
  reviewsPassed: Seq[String] = Seq(),
  reviewsPassedOther: Option[String] = None,
  sharedInCollections: Seq[ObjectId] = Seq(),
  standards: Seq[String] = Seq(),
  supportingMaterials: Seq[Resource] = Seq(),
  taskInfo: Option[TaskInfo] = None,
  workflow: Option[Workflow] = None)

  extends Content[VersionedId[ObjectId]] with EntityWithVersionedId[ObjectId] {

  def cloneItem(newCollectionId: String = collectionId): Item = {

    require(ObjectId.isValid(newCollectionId), s"$newCollectionId is not a valid ObjectId")

    val taskInfoCopy = taskInfo
      .getOrElse(
        TaskInfo(title = Some(""))).cloneInfo("[copy]")

    copy(
      id = VersionedId(ObjectId.get(), Some(0)),
      clonedFromId = Some(this.id),
      collectionId = newCollectionId,
      taskInfo = Some(taskInfoCopy),
      published = false)
  }

  /** We're going to update this with a flag **/
  def createdByApiVersion: Int = (hasQti, hasPlayerDefinition) match {
    case (true, _) => 1
    case (false, true) => 2
    case (false, false) => -1
  }

  def hasPlayerDefinition = playerDefinition.isDefined

  def hasQti: Boolean = this.data.map { d =>
    d.files.exists(f => f.isMain && f.name == Item.QtiResource.QtiXml)
  }.getOrElse(false)
}

object Item {
  val contentType: String = "item"

  object QtiResource {
    val QtiXml = "qti.xml"
  }

  object Keys {

    val author = "author"
    val bloomsTaxonomy = "bloomsTaxonomy"
    val collectionId = "collectionId"
    val contentType = "contentType"
    val contributor = "contributor"
    val contributorDetails = "contributorDetails"
    val copyright = "copyright"
    val copyrightExpirationDate = "copyrightExpirationDate"
    val copyrightImageName = "copyrightImageName"
    val copyrightOwner = "copyrightOwner"
    val copyrightYear = "copyrightYear"
    val costForResource = "costForResource"
    val credentials = "credentials"
    val credentialsOther = "credentialsOther"
    val data = "data"
    val dateModified = "dateModified"
    val depthOfKnowledge = "depthOfKnowledge"
    val description = "description"
    val extended = "extended"
    val files = "files"
    val gradeLevel = "gradeLevel"
    val id = "id"
    val itemType = "itemType"
    val keySkills = "keySkills"
    val lexile = "lexile"
    val licenseType = "licenseType"
    val originId = "originId"
    val otherAlignments = "otherAlignments"
    val playerDefinition = "playerDefinition"
    val pValue = "pValue"
    val primarySubject = "primarySubject"
    val priorGradeLevel = "priorGradeLevel"
    val priorUse = "priorUse"
    val priorUseOther = "priorUseOther"
    val published = "published"
    val relatedCurriculum = "relatedCurriculum"
    val relatedSubject = "relatedSubject"
    val reviewsPassed = "reviewsPassed"
    val reviewsPassedOther = "reviewsPassedOther"
    val sharedInCollections = "sharedInCollections"
    val sourceUrl = "sourceUrl"
    val standards = "standards"
    val subjects = "subjects"
    val supportingMaterials = "supportingMaterials"
    val taskInfo = "taskInfo"
    val title = "title"
    val workflow = "workflow"
  }
}
