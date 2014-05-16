package controllers

import java.util.concurrent.TimeUnit

import scala.{Right, Some}
import scala.concurrent.Future
import scala.concurrent.duration.Duration

import com.mongodb.casbah.Imports._
import com.novus.salat._
import dao.ModelCompanion
import org.corespring.assets.{CorespringS3Service, CorespringS3ServiceExtended}
import org.corespring.platform.core.models._
import org.corespring.platform.core.models.auth.{AccessToken, ApiClient}
import org.corespring.platform.core.models.error.CorespringInternalError
import org.corespring.platform.core.models.item.FieldValue
import org.corespring.platform.core.models.itemSession.DefaultItemSession
import play.api.Play.current
import play.api.cache.Cache
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsObject, JsString}
import play.api.mvc.{Action, Controller}

class SystemCheck(s3: CorespringS3Service) extends Controller {
  implicit val as = Akka.system

  def checkCache(): Either[CorespringInternalError, Unit] = {
    Cache.set("test", "test")
    Cache.get("test") match {
      case Some(test) => if (test == "test") Right(())
      else Left(CorespringInternalError("did not retrieve correct value from cache"))
      case None => Left(CorespringInternalError("could not retrieve any value from cache"))
    }
  }

  def checkS3(): Either[CorespringInternalError, Unit] = {
    if (s3.online) Right(())
    else Left(CorespringInternalError("S3 is not available"))
  }

  def checkDatabase(): Either[CorespringInternalError, Unit] = {
    val dbmodels: Seq[ModelCompanion[_, ObjectId]] = Seq[ModelCompanion[_, ObjectId]](
      AccessToken,
      ApiClient,
      ContentCollection,
      FieldValue,
      DefaultItemSession,
      Organization,
      Standard,
      Subject,
      User)
    dbmodels.foldRight[Either[CorespringInternalError, Unit]](Right(()))((dbmodel, result) => {
      if (result.isRight) {
        dbmodel.findOne(MongoDBObject()) match {
          case Some(_) => Right(())
          case None => Left(CorespringInternalError("could not find collection: " + dbmodel.dao.collection.getName()))
        }
      } else result
    })
  }

  def index = Action.async {

    val timeout = play.api.libs.concurrent.Promise.timeout("Oops", Duration(6, TimeUnit.SECONDS))

    val runChecks: Future[Either[CorespringInternalError, Unit]] = scala.concurrent.Future {
      val results = List(checkS3(), checkCache(), checkDatabase())

      def isAnError(result: Either[CorespringInternalError, Unit]) = result match {
        case Left(_) => true
        case Right(_) => false
      }
      val errors = results.filter(isAnError)

      if (errors.length == 0) Right() else Left(CorespringInternalError(".."))
    }

    Future.firstCompletedOf(Seq(runChecks, timeout)).map {
      case timeout: String => BadRequest("timeout")
      case Right(_) => Ok
      case Left(error: CorespringInternalError) => InternalServerError(JsObject(Seq("error" -> JsString("a check failed"), "moreInfo" -> JsString(error.message))))
      case Left(_) => BadRequest("An unknown error occured")
    }
  }
}

object SystemCheck extends SystemCheck(CorespringS3ServiceExtended)
