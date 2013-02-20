package controllers

import play.api.mvc.{Action, Controller}
import com.novus.salat._
import dao.ModelCompanion
import models.auth.{ApiClient, AccessToken}
import com.mongodb.casbah.Imports._
import models._
import item.FieldValue
import itemSession.ItemSession
import scala.Right
import akka.dispatch.{ExecutionContext, Future, Await}
import akka.util.duration._
import java.util.concurrent.{TimeUnit, TimeoutException}
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.libs.json.{JsString, JsObject}
import play.api.cache.Cache

object SystemCheck extends Controller{
  implicit val as = Akka.system
  private val checks:Seq[()=>Either[InternalError,Unit]] = Seq(
    () => checkDatabase,
    () => checkS3
  )
  def checkCache:Either[InternalError,Unit] = {
    Cache.set("test","test")
    Cache.get("test") match {
      case Some(test) => if(test == "test") Right(())
        else Left(InternalError("did not retrieve correct value from cache"))
      case None => Left(InternalError("could not retrieve any value from cache"))
    }
  }
  def checkS3:Either[InternalError,Unit] = {
    if (ConcreteS3Service.online) Right(())
    else Left(InternalError("S3 is not available"))
  }
  def checkDatabase:Either[InternalError,Unit] = {
    val dbmodels:Seq[ModelCompanion[_,ObjectId]] = Seq(
      AccessToken,
      ApiClient,
      ContentCollection,
      DbVersion,
      FieldValue,
      ItemSession,
      Organization,
      Standard,
      Subject,
      User
    )
    dbmodels.foldRight[Either[InternalError,Unit]](Right(()))((dbmodel,result) => {
      if (result.isRight){
        dbmodel.findOne(MongoDBObject()) match {
          case Some(_) => Right(())
          case None => Left(InternalError("could not find collection: "+dbmodel.dao.collection.getName()))
        }
      } else result
    })
  }

  def index = Action {
    checks.foldRight[Either[InternalError,Unit]](Right(()))((check,result) => {
      if(result.isRight){
        try{
          Await.result(Future{check()},6 second)
        } catch {
          case e:TimeoutException => Left(InternalError("timeout occurred when running system check"))
        }
      } else result
    }) match {
      case Right(_) => Ok
      case Left(error) => InternalServerError(JsObject(Seq("error" -> JsString("a check failed"),"moreInfo" -> JsString(error.message))))
    }
  }
}
