package tests

import _root_.common.seed.SeedDb
import _root_.models.item.resource.StoredFile
import controllers.{S3Service, S3ServiceClient}
import helpers.TestS3Service
import org.specs2.specification.{Step, Fragments}
import com.mongodb.casbah.Imports._

class BaseTestWithS3Service extends BaseTest with S3ServiceClient{

  def s3Service: S3Service = TestS3Service

  def initS3 = {
    TestS3Service.init
    val s3files = TestS3Service.files(s3Service.bucket)

    itemService.find(MongoDBObject()).foreach(item => {
      val storedFiles:Seq[StoredFile] =
        item.data.map(r => r.files.filter(_.isInstanceOf[StoredFile]).map(_.asInstanceOf[StoredFile])).getOrElse(Seq()) ++
          item.supportingMaterials.map(r => r.files.filter(_.isInstanceOf[StoredFile]).map(_.asInstanceOf[StoredFile])).flatten
      storedFiles.foreach(sf => {
        if(!s3files.contains(sf.storageKey)){

        }
      })
    })
  }
  def emptyData = SeedDb.emptyData()
  def emptyS3 = {

  }
  override def map(fs: => Fragments) = Step(initDB) ^ Step(initS3) ^ fs ^ Step(emptyS3)

}
