package web.controllers

import java.util.concurrent.TimeUnit
import java.lang.StringBuilder

import scala.{Right, Some}
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration

import global.Global.main

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.s3.AmazonS3Client

import com.mongodb.casbah.{ MongoURI, MongoDB, MongoConnection }
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.BasicDBObject

import org.corespring.elasticsearch.WSClient

import org.corespring.models.error.CorespringInternalError

import play.api.Play.current
import play.api.cache.Cache
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.Response
import play.api.libs.json.{JsObject, JsString}
import play.api.mvc.{Action, Controller}

class SystemCheck() extends Controller {

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
        case e: Throwable => Left(CorespringInternalError("S3 is not available"))
    }
  }

  def checkDatabase(): Either[CorespringInternalError, Unit] = {

    val mainDb = main.db
    val dbmodels = Seq(
      "accessTokens",
      "apiClients",
      "contentcolls",
      "fieldValues",
      "itemsessions",
      "orgs",
      "subjects",
      "users"
    )
    dbmodels.foldRight[Either[CorespringInternalError, Unit]](Right(()))((dbmodel, result) => {
      if (result.isRight) {
        mainDb.getCollection(dbmodel).findOne() match {
          case _: BasicDBObject => Right(())
          case _ => Left(CorespringInternalError("could not find collection: " + dbmodel))
        }
      } else result
    })
  }

  def checkElasticSearch(): Either[CorespringInternalError, Unit] = {

    val cfg = main.elasticSearchConfig
    val elasticSearchClient = WSClient(cfg.url)
    val elasticClientResult = elasticSearchClient.request("_cluster/health").get.flatMap[Either[CorespringInternalError, Unit]]( response => Future {
        response.status match {
          case 200 => Right(())
          case _ => Left(CorespringInternalError("could not connect to ElasticSearch provider"))
        }
      } recover {
        case timeout: java.util.concurrent.TimeoutException => Left(CorespringInternalError("Timed out while connecting to ElasticSearch cluster"))
    })
    Await.result(elasticClientResult, Duration(5, TimeUnit.SECONDS));
  }


  def index = Action.async {

    val timeout = play.api.libs.concurrent.Promise.timeout("Oops", Duration(6, TimeUnit.SECONDS))

    val runChecks: Future[Either[CorespringInternalError, Unit]] = scala.concurrent.Future {
      val results = List(
        checkS3(),
        checkCache(),
        checkDatabase(),
        checkElasticSearch()
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
              case _ => Ok
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