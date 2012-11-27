package controllers

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.auth.PropertiesCredentials
import play.api.Play
import com.amazonaws.services.s3.model._
import play.api.mvc._
import play.api.http.HeaderNames._
import play.api.Play.current
import java.io._
import play.api.libs.iteratee.{Input, Done, Iteratee}
import actors.{IScheduler, TIMEOUT, Actor}
import actors.scheduler.ResizableThreadPoolScheduler
import com.amazonaws.{AmazonServiceException, AmazonClientException}
import play.api.libs.iteratee.{Input, Done, Enumerator, Iteratee}
import api.ApiError
import play.api.libs.json.Json

object S3Service {

  private var optS3: Option[AmazonS3Client] = None

  case class S3DeleteResponse(success: Boolean, key: String, msg: String = "")

  /**
   * Init the S3 client
   * @param propertiesFile the properties file that contains the amazon credentials
   */
  def init(propertiesFile: File): Unit = this.synchronized {
    try {
      optS3 = Some(new AmazonS3Client(new PropertiesCredentials(propertiesFile)))
    } catch {
      case e: IOException => InternalError("unable to authenticate s3 server with given credentials", LogType.printFatal)
    }
  }


  /**
   * handle file upload through multiple parts. See http://docs.amazonwebservices.com/AmazonS3/latest/dev/llJavaUploadFile.html
   * handle the upload stream using an iteratee using the BodyParser. See https://github.com/playframework/Play20/wiki/ScalaBodyParsers
   * @param bucket the name of the amazon bucket
   * @param keyName
   * @return
   */
  def s3upload(bucket: String, keyName: String): BodyParser[Int] = BodyParser("s3 file upload") {
    request =>
      val optContentLength = request.headers.get(CONTENT_LENGTH)
      optContentLength match {
        case Some(contentLength) => try {
          s3UploadSingle(bucket, keyName, contentLength.toInt)
        } catch {
          case e: Exception => Done[Array[Byte], Either[Result, Int]](Left(Results.InternalServerError(Json.toJson(ApiError.ContentLength(Some("value in Content-Length not valid integer"))))), Input.Empty)
        }
        case None => Done[Array[Byte], Either[Result, Int]](Left(Results.InternalServerError(Json.toJson(ApiError.ContentLength(Some("no Content-Length specified"))))), Input.Empty)
      }
  }

  /**
   * @return
   */
  def s3download(bucket: String, itemId: String, keyName: String): Result = download(bucket, itemId + "/" + keyName)

  def download(bucket: String, fullKey: String, headers: Option[Headers] = None): Result = {

    require(fullKey != null && !fullKey.isEmpty, "Invalid key")
    require(bucket != null && !bucket.isEmpty, "Invalid bucket")

    def returnResultWithAsset(s3 : AmazonS3Client, bucket: String, key: String) : Result = {
      val s3Object: S3Object = s3.getObject(bucket, fullKey) //get object. may result in exception
      val inputStream: InputStream = s3Object.getObjectContent
      val objContent: Enumerator[Array[Byte]] = Enumerator.fromStream(inputStream)
      val metadata = s3Object.getObjectMetadata
      SimpleResult(
        header = ResponseHeader(200,
          Map(CONTENT_LENGTH -> metadata.getContentLength.toString,
            ETAG -> metadata.getETag)),
        body = objContent
      )
    }

    def returnNotModifiedOrResultWithAsset( s3 : AmazonS3Client, headers: Headers, bucket: String, key: String): Result = {
      val metadata: ObjectMetadata = s3.getObjectMetadata(new GetObjectMetadataRequest(bucket, fullKey))
      val ifNoneMatch = headers.get(IF_NONE_MATCH).getOrElse("")
      if (ifNoneMatch != "" && ifNoneMatch == metadata.getETag) {
        Results.NotModified
      }
      else {
        returnResultWithAsset(s3,bucket, fullKey)
      }
    }

    optS3 match {
      case Some(s3) => {
        try {
          headers match {
            case Some(foundHeaders) => returnNotModifiedOrResultWithAsset(s3, foundHeaders, bucket, fullKey)
            case _ => returnResultWithAsset(s3, bucket, fullKey)
          }
        }
        catch {
          case e: AmazonClientException =>
            Log.f("AmazonClientException in s3download: " + e.getMessage)
            Results.InternalServerError(Json.toJson(ApiError.AmazonS3Client(Some("Occurred when attempting to retrieve object: " + fullKey))))
          case e: AmazonServiceException =>
            Log.e("AmazonServiceException in s3download: " + e.getMessage)
            Results.InternalServerError(Json.toJson(ApiError.AmazonS3Server(Some("Occurred when attempting to retrieve object: " + fullKey))))
        }
      }
      case None =>
        Log.f("amazon s3 service not initialized")
        Results.InternalServerError(Json.toJson(ApiError.S3NotIntialized))
    }
  }


  def delete(bucket: String, keyName: String): S3DeleteResponse = {

    optS3 match {
      case Some(s3) => {
        try {
          val s3obj: S3Object = s3.getObject(bucket, keyName) //get object. may result in exception
          s3.deleteObject(bucket, s3obj.getKey())
          S3DeleteResponse(true, keyName)
        } catch {
          case e: AmazonClientException =>
            Log.f("AmazonClientException in delete: " + e.getMessage)
            S3DeleteResponse(false, keyName, e.getMessage)
          case e: AmazonServiceException =>
            Log.e("AmazonServiceException in delete: " + e.getMessage)
            S3DeleteResponse(false, keyName, e.getMessage)
        }
      }
      case _ => S3DeleteResponse(false, keyName, "Error with S3 Service")
    }
  }

  /**
   * create an actor for the s3 upload. pipe the data as it comes in to the s3 actor.
   * @param bucket the name of the amazon bucket
   * @param keyName
   * @param contentLength
   * @return
   */
  private def s3UploadSingle(bucket: String, keyName: String, contentLength: Int): Iteratee[Array[Byte], Either[Result, Int]] = {
    optS3 match {
      case Some(s3) => {
        val outputStream = new PipedOutputStream()
        val s3Writer = new S3Writer(bucket, keyName, new PipedInputStream(outputStream), contentLength)
        s3Writer.start()
        try {
          s3Writer ! Begin //initiate upload. S3Writer will now wait for data chunks to be pushed to it's input stream
        } catch {
          case e: IOException => Log.f("error occurred when creating pipe")
        }
        Iteratee.fold[Array[Byte], Either[Result, Int]](Right(0)) {
          (result, chunk) =>
            result match {
              case Right(acc) => try {
                outputStream.write(chunk, 0, chunk.size)
                Right(acc + chunk.size)
              } catch {
                case e: IOException =>
                  Log.f("IOException occurred when writing to S3: " + e.getMessage)
                  Left(Results.InternalServerError(Json.toJson(ApiError.S3Write)))
              }
              case Left(error) => Left(error)
            }
        }.mapDone(result => {
          try {
            outputStream.close()
          } catch {
            case e: IOException =>
          }
          result match {
            case Right(acc) => {
              s3Writer !?(5000, EOF) match {
                case Some(Ack(s3reply)) => s3reply match {
                  case Right(_) => Right(acc)
                  case Left(error) => Left(Results.InternalServerError(Json.toJson(ApiError.S3Write(error.clientOutput))))
                }
                case None => Left(Results.InternalServerError(Json.toJson(ApiError.S3Write(Some("timeout occured before S3Writer could return")))))
                case _ => Left(Results.InternalServerError(Json.toJson(ApiError.S3Write(Some("unknown reply from S3Writer")))))
              }
            }
            case Left(error) => Left(error)
          }
        })
      }
      case None => Done[Array[Byte], Either[Result, Int]](Left(Results.InternalServerError("s3 instance not initialized")), Input.Empty)
    }
  }

  private case object Begin

  private case object EOF

  private case class Ack(result: Either[InternalError, Unit])

  private class S3Writer(bucket: String, keyName: String, inputStream: InputStream, contentLength: Int) extends Actor {
    def act() {
      var errorOccurred: Option[InternalError] = None
      while (true) {
        receiveWithin(60000) {
          case Begin => {
            val objectMetadata = new ObjectMetadata;
            objectMetadata.setContentLength(contentLength)
            try {
              optS3.get.putObject(bucket, keyName, inputStream, objectMetadata) // assume optS3 has instance of S3 otherwise this would have never been called
            } catch {
              case e: Exception => {
                Log.f("exception occurred in Begin of S3Writer: " + e.getMessage)
                try {
                  inputStream.close()
                } catch {
                  case e: IOException => Log.f("IOException when closing input stream in S3Writer: " + e.getMessage)
                }
                errorOccurred = Some(InternalError("error writing to S3", LogType.printFatal))
              }
            }
          }
          case EOF => {
            try {
              inputStream.close()
            } catch {
              case e: IOException => Log.f("IOException when closing input stream in S3Writer: " + e.getMessage)
            }
            errorOccurred match {
              case Some(error) => Actor.reply(Ack(Left(error)))
              case None => Actor.reply(Ack(Right()));
            }
            exit()
          }
          case TIMEOUT => {
            try {
              inputStream.close()
            } catch {
              case e: IOException =>
            }
            exit()
          }
        }
      }
    }

    override def scheduler: IScheduler = new ResizableThreadPoolScheduler()
  }

}
