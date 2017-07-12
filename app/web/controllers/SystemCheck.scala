package web.controllers

import java.util.concurrent.TimeUnit

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model._
import com.mongodb.BasicDBObject
import com.mongodb.casbah.MongoDB

import org.corespring.elasticsearch.WSClient
import org.corespring.itemSearch.ElasticSearchConfig
import org.corespring.models.error.CorespringInternalError

import play.api.Play.current
import play.api.cache.Cache
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{ JsObject, JsString }
import play.api.mvc.{ Action, Controller }

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }
import scala.util.{ Failure, Success }

class SystemCheck(s3: AmazonS3, db: MongoDB, elasticSearchConfig: ElasticSearchConfig) extends Controller {

  val timeUnit = Duration(5, TimeUnit.SECONDS)

  def checkCache(setCache: String, getCache: String): Future[Either[CorespringInternalError, Unit]] = Future {
    Cache.set(setCache, setCache)
    Cache.get(getCache) match {
      case Some(test) => if (test == "test") Right(())
      else Left(CorespringInternalError("did not retrieve correct value from cache"))
      case None => Left(CorespringInternalError("could not retrieve any value from cache"))
    }
  }

  def checkS3(_s3: AmazonS3, bucket: String): Future[Either[CorespringInternalError, Unit]] = Future {
    try {
      val testBucket = new HeadBucketRequest(bucket)
      _s3.headBucket(testBucket)
      Right(())
    } catch {
      case e: Throwable => Left(CorespringInternalError("S3 / Bucket is not available"))
    }
  }

  def checkDatabase(_db: MongoDB, dbmodels: Seq[String]): Future[Either[CorespringInternalError, Unit]] = Future {
    dbmodels.foldRight[Either[CorespringInternalError, Unit]](Right(()))((dbmodel, result) => {
      if (result.isRight) {
        if (_db.collectionExists(dbmodel)) Right(())
        else Left(CorespringInternalError("could not find collection: " + dbmodel))
      } else result
    })
  }

  def checkElasticSearch(_elasticSearchConfig: ElasticSearchConfig): Future[Either[CorespringInternalError, Unit]] = Future {
    val elasticSearchClient = WSClient(_elasticSearchConfig.url)
    val elasticClientResult = elasticSearchClient.request("_cluster/health").get.flatMap[Either[CorespringInternalError, Unit]](response => Future {
      response.status match {
        case 200 => Right(())
        case _ => Left(CorespringInternalError("could not connect to ElasticSearch provider"))
      }
    } recover {
      case timeout: java.util.concurrent.TimeoutException => Left(CorespringInternalError("Timed out while connecting to ElasticSearch cluster"))
    })
    Await.result(elasticClientResult, timeUnit);
  }


  def index = Action.async {  

    val timeoutCheck = play.api.libs.concurrent.Promise.timeout("Ooops", timeUnit).map {
      case timeout: String =>  Left(CorespringInternalError("Timed out"))
    }

    val testCache = "test"
    val bucket = "corespring-system-check"
    val dbmodels = Seq(
      "accessTokens",
      "apiClients",
      "contentcolls",
      "fieldValues",
      "itemsessions",
      "orgs",
      "subjects",
      "users")

    val futureCheckCache = checkCache(testCache,testCache)
    val futureCheckS3 = checkS3(s3, bucket)
    val futureCheckDatabase = checkDatabase(db, dbmodels)
    val futureCheckElasticSearch = checkElasticSearch(elasticSearchConfig)

    def isAnError(result: Either[CorespringInternalError, Unit]) = result match {
      case Left(_) => true
      case Right(_) => false
    }

    val results = Future.sequence(
      Seq(
        Future.firstCompletedOf(Seq(futureCheckCache, timeoutCheck)),
        Future.firstCompletedOf(Seq(futureCheckS3, timeoutCheck)),
        Future.firstCompletedOf(Seq(futureCheckDatabase, timeoutCheck)),
        Future.firstCompletedOf(Seq(futureCheckElasticSearch, timeoutCheck))
      )
    )

    results.map { res =>
      val errors = res.filter(isAnError)

      if (errors.length == 0) {
        Ok
      } else {
        val sb = new java.lang.StringBuilder
        errors.foreach { e =>
          e match {
            case Left(x) => {
              sb.append(x.message + "; ")
            }
            case _ => Ok
          }
        }
        InternalServerError(JsObject(Seq("error" -> JsString("check(s) failed"), "moreInfo" -> JsString(sb.toString()))))
      }
    }
  }
}