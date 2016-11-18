package org.corespring.models.item

import org.bson.types.ObjectId
import org.corespring.models.item.resource.Resource
import org.corespring.platform.data.mongo.models.{ EntityWithVersionedId, VersionedId }
import org.joda.time.DateTime

case class ItemStandards(title: String, standards: Seq[String], id: VersionedId[ObjectId])

class Item(
  var collectionId: String,
  var originId: Option[String] = None,
  var clonedFromId: Option[VersionedId[ObjectId]] = None,
  var contentType: String = Item.contentType,
  var contributorDetails: Option[ContributorDetails] = None,
  var data: Option[Resource] = None,
  var dateModified: Option[DateTime] = Some(new DateTime()),
  var id: VersionedId[ObjectId] = VersionedId(ObjectId.get(), Some(0)),
  var lexile: Option[String] = None,
  var otherAlignments: Option[Alignments] = None,
  var playerDefinition: Option[PlayerDefinition] = None,
  var priorGradeLevels: Seq[String] = Seq(),
  var priorUse: Option[String] = None,
  var priorUseOther: Option[String] = None,
  var published: Boolean = false,
  var pValue: Option[String] = None,
  var reviewsPassed: Seq[String] = Seq(),
  var reviewsPassedOther: Option[String] = None,
  var sharedInCollections: Seq[ObjectId] = Seq(),
  var standards: Seq[String] = Seq(),
  var supportingMaterials: Seq[Resource] = Seq(),
  var taskInfo: Option[TaskInfo] = None,
  var workflow: Option[Workflow] = None)

  extends Content[VersionedId[ObjectId]] with EntityWithVersionedId[ObjectId] {

  def cloneItem(newCollectionId: String = collectionId): Item = {

    require(ObjectId.isValid(newCollectionId), s"$newCollectionId is not a valid ObjectId")

    val taskInfoCopy = taskInfo
      .getOrElse(
        TaskInfo(title = Some(""))).cloneInfo("[copy]")

    new Item(
      id = VersionedId(ObjectId.get(), Some(0)),
      clonedFromId = Some(this.id),
      collectionId = newCollectionId,
      taskInfo = Some(taskInfoCopy),
      published = false)
  }

  def copy(
    collectionId: String = null,
    originId: Option[String] = null,
    clonedFromId: Option[VersionedId[ObjectId]] = null,
    contentType: String = null,
    contributorDetails: Option[ContributorDetails] = null,
    data: Option[Resource] = null,
    dateModified: Option[DateTime] = null,
    id: VersionedId[ObjectId] = null,
    lexile: Option[String] = null,
    otherAlignments: Option[Alignments] = null,
    playerDefinition: Option[PlayerDefinition] = null,
    priorGradeLevels: Seq[String] = null,
    priorUse: Option[String] = null,
    priorUseOther: Option[String] = null,
    published: Option[Boolean] = None,
    pValue: Option[String] = null,
    reviewsPassed: Seq[String] = null,
    reviewsPassedOther: Option[String] = null,
    sharedInCollections: Seq[ObjectId] = null,
    standards: Seq[String] = null,
    supportingMaterials: Seq[Resource] = null,
    taskInfo: Option[TaskInfo] = null,
    workflow: Option[Workflow] = null): Item = {

    val item = new Item(this.collectionId)
    item.collectionId = Option(collectionId).getOrElse(this.collectionId)
    item.originId = Option(originId).getOrElse(this.originId)
    item.clonedFromId = Option(clonedFromId).getOrElse(this.clonedFromId)
    item.contentType = Option(contentType).getOrElse(this.contentType)
    item.contributorDetails = Option(contributorDetails).getOrElse(this.contributorDetails)
    item.data = Option(data).getOrElse(this.data)
    item.dateModified = Option(dateModified).getOrElse(this.dateModified)
    item.id = Option(id).getOrElse(this.id)
    item.lexile = Option(lexile).getOrElse(this.lexile)
    item.otherAlignments = Option(otherAlignments).getOrElse(this.otherAlignments)
    item.playerDefinition = Option(playerDefinition).getOrElse(this.playerDefinition)
    item.priorGradeLevels = Option(priorGradeLevels).getOrElse(this.priorGradeLevels)
    item.priorUse = Option(priorUse).getOrElse(this.priorUse)
    item.priorUseOther = Option(priorUseOther).getOrElse(this.priorUseOther)
    item.published = published.getOrElse(this.published)
    item.pValue = Option(pValue).getOrElse(this.pValue)
    item.reviewsPassed = Option(reviewsPassed).getOrElse(this.reviewsPassed)
    item.reviewsPassedOther = Option(reviewsPassedOther).getOrElse(this.reviewsPassedOther)
    item.sharedInCollections = Option(sharedInCollections).getOrElse(this.sharedInCollections)
    item.standards = Option(standards).getOrElse(this.standards)
    item.supportingMaterials = Option(supportingMaterials).getOrElse(this.supportingMaterials)
    item.taskInfo = Option(taskInfo).getOrElse(this.taskInfo)
    item.workflow = Option(workflow).getOrElse(this.workflow)
    item
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

  def apply(
    collectionId: String,
    originId: Option[String] = None,
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
    workflow: Option[Workflow] = None): Item = {
    new Item(collectionId, originId, clonedFromId, contentType, contributorDetails, data, dateModified, id, lexile,
      otherAlignments, playerDefinition, priorGradeLevels, priorUse, priorUseOther, published, pValue, reviewsPassed,
      reviewsPassedOther, sharedInCollections, standards, supportingMaterials, taskInfo, workflow)
  }

}
