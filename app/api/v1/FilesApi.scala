package api.v1


import com.typesafe.config.ConfigFactory
import com.codahale.jerkson.Json.generate
import controllers.S3Service
import controllers.auth.BaseApi

case class UploadResponse(successful:Boolean, message : String, itemId : String, fileName : String = "")
case class DeleteResponse(successful:Boolean)

object FilesApi extends BaseApi
{
  private final val AMAZON_BASE_URL : String = ConfigFactory.load().getString("AMAZON_BASE_URL")
  private final val AMAZON_ASSETS_BUCKET : String = ConfigFactory.load().getString("AMAZON_ASSETS_BUCKET")

  val Json = ("Content-Type" -> "application/json; charset=utf-8")

  /**
   * Upload a file to S3
   * @param itemId
   * @param fileName
   * @return
   */
  def upload(itemId:String, fileName: String) = ApiAction(S3Service.s3upload(AMAZON_ASSETS_BUCKET, itemId + "/" + fileName)) { request =>
    val url : String = AMAZON_BASE_URL+ "/" + AMAZON_ASSETS_BUCKET + "/" + itemId + "/" + fileName
    val responseJson : String = generate(UploadResponse(true, "upload successful", itemId, fileName))
    Ok(responseJson).withHeaders(Json)
  }

  /**
   * Return the file from S3
   * @param itemId
   * @param fileName
   * @return
   */
  def getFile(itemId : String, fileName : String) = ApiAction { request =>
    S3Service.s3download(AMAZON_ASSETS_BUCKET, itemId, fileName)
  }

  /**
   * Delete the file from S3
   * @param itemId
   * @param fileName
   * @return
   */
  def delete(itemId:String, fileName:String) = ApiAction{ request =>
    val response : S3Service.S3DeleteResponse = S3Service.delete( AMAZON_ASSETS_BUCKET, itemId + "/" + fileName )
    Ok(generate(response)).withHeaders(Json)
  }
}
