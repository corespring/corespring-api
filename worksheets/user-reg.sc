/**
 * An example of adding a user to CoreSpring.
 * We add 'ian.henderson' to 'Demo Organization'
 * After running this you can log in as 'ian.henderson' to the web app.
 */

import java.io.File
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.s3.{AmazonS3Client, AmazonS3}
import com.mongodb.casbah.{MongoConnection, MongoURI, MongoDB}
import com.novus.salat.Context
import org.bson.types.ObjectId
import org.corespring.hash.BCryptHasher
import org.corespring.models.appConfig.{ArchiveConfig, Bucket, AccessTokenConfig}
import org.corespring.salat.config.SalatContext
import org.corespring.services.salat.bootstrap.{SalatServicesExecutionContext, SalatServices}
import org.joda.time.DateTime
import play.api.Configuration
import scala.concurrent.ExecutionContext

import org.corespring.models.{ UserOrg, User}
import org.corespring.models.auth.Permission

import scalaz.{Success,Failure}

lazy val conf = {
  val f = new File("conf/application.conf")
  require(f.exists, "conf file doesn't exist?")
  val u = com.typesafe.config.ConfigFactory.parseFile(f)
  val out = new Configuration(u.resolve())
  out
}

val services = new SalatServices {
  override def archiveConfig: ArchiveConfig = {
    (for{
      cId <- conf.getString("archive.contentCollectionId")
      orgId <- conf.getString("archive.orgId")
    } yield {
      ArchiveConfig(new ObjectId(cId), new ObjectId(orgId))
    }).get
  }

  lazy val uri = {
    val uriString = conf.getString("mongo.default.uri").getOrElse("mongodb://localhost/api")
    MongoURI(uriString)
  }

  lazy val connection = MongoConnection(uri)

  override lazy val db: MongoDB =  connection(uri.database.get)

  override def bucket: Bucket = Bucket("bucket")

  lazy val awsCredentials: AWSCredentials = new AWSCredentials {
    override lazy val getAWSAccessKeyId: String = conf.getString("AMAZON_ACCESS_KEY").get
    override lazy val getAWSSecretKey: String = conf.getString("AMAZON_ACCESS_SECRET").get
  }

  override lazy val s3: AmazonS3 = new AmazonS3Client(awsCredentials)

  override def mostRecentDateModifiedForSessions: (Seq[ObjectId]) => Option[DateTime] = _ => None

  override def accessTokenConfig: AccessTokenConfig = AccessTokenConfig()

  override def salatServicesExecutionContext: SalatServicesExecutionContext = SalatServicesExecutionContext(ExecutionContext.global)

  override implicit def context: Context = new SalatContext(this.getClass.getClassLoader)
}

val orgs = services.orgService.list()

val result = orgs.find(_.name == "Demo Organization").map{ o => 


  val hasher = new BCryptHasher()
  val password = hasher.hash("hello").hashed
  val userOrg = UserOrg(o.id, 3)

  val user = User(
    "ian.henderson", 
    "Ian Henderson", 
    "ian@irfu.ie", 
    org = userOrg,
    password = password)

  services.userService.insertUser(user, o.id, Permission.Write) match{
    case Success(u) => s"Successfully inserted user: $u"
    case Failure(e) => e.message
  }

}



