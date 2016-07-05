package web.controllers

import java.util.concurrent.TimeUnit
import java.lang.StringBuilder

import scala.{Right, Some}
import scala.concurrent.Future
import scala.concurrent.duration.Duration

import global.Global.main

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.s3.AmazonS3Client

import com.mongodb.casbah.{ MongoURI, MongoDB, MongoConnection }
import com.mongodb.casbah.commons.MongoDBObject


import org.corespring.itemSearch
import org.corespring.itemSearch.ItemIndexService
import org.corespring.itemSearch.ItemIndexQuery

import org.corespring.elasticsearch.ContentIndexer
import org.corespring.models.error.CorespringInternalError

import play.api.Play.current
import play.api.cache.Cache
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsObject, JsString}
import play.api.mvc.{Action, Controller}

class SystemCheck() extends Controller {

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
    try {
      val client = main.s3
      val testBucket = "corespring-system-check"
      val testObject = "circle.png"
      val getS3Object = client.getObject(testBucket, testObject)
      Right(())
    } catch {
        case e: Throwable => false
        Left(CorespringInternalError("S3 is not available"))
    }
  }


  def checkDatabase(): Either[CorespringInternalError, Unit] = {

    lazy val mainDb = main.db
    
    /*
    lazy val mongoUri = "mongodb://corespring:cccrcId4d@ds035160-a0.mongolab.com:35160,ds035160-a1.mongolab.com:35160/corespring-qa"
    lazy val uri = MongoURI(mongoUri)
    lazy val connection: MongoConnection = MongoConnection(uri)
    lazy val db: MongoDB = connection.getDB(uri.database.get)
    
    db.collectionNames.foreach { n =>
      println(n)
    }
    */
    
    val dbmodels = Seq(
      "accessTokens",
      "apiClients",
      "contentcolls",
      "fieldValues",
      "itemSessions",
      "orgs",
      "subjects",
      "users")

    dbmodels.foldRight[Either[CorespringInternalError, Unit]](Right(()))((dbmodel, result) => {
      if (result.isRight) {
        mainDb.getCollection(dbmodel).findOne(MongoDBObject()) match {
          case _: MongoDBObject => Right(())
          case _ => Left(CorespringInternalError("could not find collection: " + dbmodel))
        }
      } else result
    })
  }

  def checkElasticSearch(): Either[CorespringInternalError, Unit] = {
    val cfg = main.elasticSearchConfig
    //println(cfg.url)
    //println(cfg.mongoUri)
    //println(cfg.componentPath)
    val query = ItemIndexQuery(text = Some("test-cluster"), collections = Seq(collectionId.toString))
    val itemIndexService = main.itemIndexService
    println (itemIndexService)

    Right(())
  }

  def index = Action.async {

    val timeout = play.api.libs.concurrent.Promise.timeout("Oops", Duration(6, TimeUnit.SECONDS))

    val runChecks: Future[Either[CorespringInternalError, Unit]] = scala.concurrent.Future {
      val results = List(
        checkS3(),
        checkCache(),
        checkDatabase()
        //,checkElasticSearch()
      )

      def isAnError(result: Either[CorespringInternalError, Unit]) = result match {
        case Left(_) => true
        case Right(_) => false
      }
      val errors = results.filter(isAnError)

      if (errors.length == 0) Right() 
      else {
        val sb = new java.lang.StringBuilder
        errors.foreach { e =>
            e match {
              case Left(x) => {
                sb.append(x.message + "; ")
              }
            }
        }
        Left(CorespringInternalError(sb.toString()))
      }
    }

    Future.firstCompletedOf(Seq(runChecks, timeout)).map {
      case timeout: String => BadRequest("timeout")
      case Right(_) => Ok
      case Left(error: CorespringInternalError) => {
        InternalServerError(JsObject(Seq("error" -> JsString("a check failed"),  "moreInfo" -> JsString(error.message))))
      }
      case Left(_) => BadRequest("An unknown error occured")
    }
  }
}