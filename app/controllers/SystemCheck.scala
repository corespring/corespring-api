package controllers

import play.api.mvc.{Action, Controller}
import com.novus.salat._
import dao.ModelCompanion
import models.auth.{ApiClient, AccessToken}
import com.mongodb.casbah.Imports._
import models._
import item.FieldValue
import models.itemSession.{DefaultItemSession, ItemSession}
import scala.Right
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.cache.Cache
import scala.concurrent.Future
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsString, JsObject}

class SystemCheck(s3: CorespringS3Service) extends Controller {
  implicit val as = Akka.system


  def checkCache(): Either[InternalError, Unit] = {
    Cache.set("test", "test")
    Cache.get("test") match {
      case Some(test) => if (test == "test") Right(())
      else Left(InternalError("did not retrieve correct value from cache"))
      case None => Left(InternalError("could not retrieve any value from cache"))
    }
  }

  def checkS3(): Either[InternalError, Unit] = {
    if (s3.online) Right(())
    else Left(InternalError("S3 is not available"))
  }

  def checkDatabase(): Either[InternalError, Unit] = {
    val dbmodels: Seq[ModelCompanion[_, ObjectId]] = Seq(
      AccessToken,
      ApiClient,
      ContentCollection,
      FieldValue,
      DefaultItemSession,
      Organization,
      Standard,
      Subject,
      User
    )
    dbmodels.foldRight[Either[InternalError, Unit]](Right(()))((dbmodel, result) => {
      if (result.isRight) {
        dbmodel.findOne(MongoDBObject()) match {
          case Some(_) => Right(())
          case None => Left(InternalError("could not find collection: " + dbmodel.dao.collection.getName()))
        }
      } else result
    })
  }

  def index = Action {

    val timeout = play.api.libs.concurrent.Promise.timeout("Oops", Duration(6, TimeUnit.SECONDS))

    val runChecks: Future[Either[InternalError, Unit]] = scala.concurrent.Future {
      val results = List(checkS3(), checkCache(), checkDatabase())

      def isAnError(result: Either[InternalError, Unit]) = result match {
        case Left(_) => true
        case Right(_) => false
      }
      val errors = results.filter(isAnError)

      if (errors.length == 0) Right() else Left(InternalError(".."))
    }

    Async {
      Future.firstCompletedOf(Seq(runChecks, timeout)).map {
        case timeout: String => BadRequest("timeout")
        case Right(_) => Ok
        case Left(error : InternalError) => InternalServerError(JsObject(Seq("error" -> JsString("a check failed"),"moreInfo" -> JsString(error.message))))
        case Left(_) => BadRequest("..")
      }
    }
  }
}

object SystemCheck extends SystemCheck(CorespringS3ServiceImpl)
