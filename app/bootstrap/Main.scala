package bootstrap

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.s3.{ AmazonS3, AmazonS3Client }
import com.mongodb.casbah.MongoDB
import com.novus.salat.Context
import common.db.Db
import org.bson.types.ObjectId
import org.corespring.drafts.item.models.OrgAndUser
import org.corespring.models.{ Standard, Subject }
import org.corespring.models.item.FieldValue
import org.corespring.models.json.JsonFormatting
import org.corespring.services.salat.ServicesContext
import org.corespring.services.salat.bootstrap._
import org.corespring.v2.api.drafts.item.ItemDraftsModule
import play.api.Play
import play.api.mvc.{ RequestHeader, Controller }

object Main extends SalatServices with ItemDraftsModule {

  import play.api.Play.current

  lazy val config = current.configuration

  lazy val aws = AwsConfig(
    config.getString("AMAZON_ACCESS_KEY").getOrElse("?"),
    config.getString("AMAZON_ACCESS_SECRET").getOrElse("?"),
    config.getString("AMAZON_BUCKET").getOrElse("?"))

  lazy val archiveConfig = ArchiveConfig(
    new ObjectId(config.getString("archive.contentCollectionId").getOrElse("?")),
    new ObjectId(config.getString("archive.orgId").getOrElse("?")))

  lazy val s3: AmazonS3 = new AmazonS3Client(new AWSCredentials {
    override def getAWSAccessKeyId: String = aws.key

    override def getAWSSecretKey: String = aws.secret
  })

  lazy val accessTokenConfig = AccessTokenConfig()

  def controllers: Seq[Controller] = Seq(itemDrafts)

  override def db: MongoDB = Db.salatDb()

  @deprecated("This is a legacy function - remove", "1.0")
  override def mode: AppMode = AppMode(Play.current.mode.toString.toLowerCase())

  override implicit def context: Context = new ServicesContext(Play.classloader)

  override def identifyUser: (RequestHeader) => Option[OrgAndUser] = ???

  override def jsonFormatting: JsonFormatting = new JsonFormatting {
    override def findStandardByDotNotation: (String) => Option[Standard] = standard.findOneByDotNotation(_)

    override def fieldValue: FieldValue = Main.this.fieldValue.get.get

    override def findSubjectById: (ObjectId) => Option[Subject] = subject.findOneById(_)
  }
}