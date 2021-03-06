package org.corespring.v2.api.drafts.item

import com.amazonaws.services.s3.AmazonS3
import com.mongodb.casbah.{ MongoCollection, MongoDB }
import com.novus.salat.Context
import org.corespring.drafts.item.models.OrgAndUser
import org.corespring.drafts.item.services.ItemDraftConfig.CollectionNames
import org.corespring.drafts.item.services.{ CommitService, ItemDraftService }
import org.corespring.drafts.item.{ ItemDraftAssets, ItemDrafts => DraftsBackend, S3ItemDraftAssets }
import org.corespring.models.appConfig.Bucket
import org.corespring.models.json.JsonFormatting
import org.corespring.services.{ OrgCollectionService, OrganizationService }
import org.corespring.services.item.ItemService
import org.corespring.v2.api.drafts.item.json.ItemDraftJson
import play.api.mvc.RequestHeader

trait ItemDraftsModule {

  import com.softwaremill.macwire.MacwireMacros._

  def itemService: ItemService

  def orgService: OrganizationService

  def orgCollectionService: OrgCollectionService

  def db: MongoDB

  def context: Context

  def draftService: ItemDraftService = new ItemDraftService {

    override def collection: MongoCollection = db(CollectionNames.itemDrafts)
    override implicit def context: Context = ItemDraftsModule.this.context
  }

  def commitService: CommitService = new CommitService {
    override def collection: MongoCollection = db(CollectionNames.commits)
    override implicit def context: Context = ItemDraftsModule.this.context
  }

  def s3: AmazonS3
  def bucket: Bucket

  def assets: ItemDraftAssets = wire[S3ItemDraftAssets]

  def itemDrafts: DraftsBackend = wire[DraftsBackend]

  def identifyUser: (RequestHeader) => Option[OrgAndUser]

  def jsonFormatting: JsonFormatting

  lazy val itemDraftJson: ItemDraftJson = wire[ItemDraftJson]

  lazy val itemDraftsController: ItemDrafts = wire[ItemDrafts]
}
