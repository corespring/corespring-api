package api.v1

import play.api.mvc.{Result, Request, BodyParser, Action}
import controllers.auth.BaseApi
import models.{StoredFile, Item}
import org.bson.types.ObjectId
import controllers.S3Service
import play.api.Logger
import com.typesafe.config.ConfigFactory
import play.api.libs.json.Json._

object ResourceApi extends BaseApi {

  private final val AMAZON_ASSETS_BUCKET: String = ConfigFactory.load().getString("AMAZON_ASSETS_BUCKET")

  /**
   * A wrapping Action that checks that an Item with the given id exists.
   * @param itemId - the item id
   * @param additionalCheck - an additional check beyond checking for the item.
   * @param action - the action to invoke
   * @tparam A
   */
  case class HasItem[A](
                         itemId: String,
                         additionalCheck: (Item => Boolean) = (i: Item) => true,
                         action: Action[A]) extends Action[A] {

    def apply(request: Request[A]): Result = {
      Item.findOneById(new ObjectId(itemId)) match {
        case Some(item) => {
          if (additionalCheck(item)) {
            action(request)
          }
          else {
            NotFound
          }
        }
        case _ => NotFound
      }
    }

    lazy val parser = action.parser
  }

  /**
   * Upload a file to the 'data' Resource in the Item.
   * @param itemId
   * @param filename
   * @return
   */
  def uploadFileToData(itemId: String, filename: String) =
    HasItem(
      itemId,
      //We have no additional check - so we just return true for the item
      (i: Item) => true,
      Action(S3Service.s3upload(AMAZON_ASSETS_BUCKET, itemId + "/resource/" + filename)) {
        request =>

          val item = Item.findOneById(new ObjectId(itemId)).get
          val resource = item.data.get

          val file = new StoredFile(
            filename,
            contentType(filename),
            false,
            itemId + "/resource/" + filename)

          resource.files = resource.files ++ Seq(file)
          Item.save(item)
          Ok(toJson(file))
      }
    )

  /**
   * Upload a file to a supporting material Resource in the item.
   * @param itemId
   * @param materialName
   * @param filename
   * @return
   */
  def uploadFile(itemId: String, materialName: String, filename: String) =
    HasItem(itemId, canFindResource(materialName)(_),

      Action(S3Service.s3upload(AMAZON_ASSETS_BUCKET, storageKey(itemId, materialName, filename))) {
        request =>

          val item = Item.findOneById(new ObjectId(itemId)).get
          val resource = item.supportingMaterials.find(_.name == materialName).get

          val file = new StoredFile(
            filename,
            contentType(filename),
            false,
            storageKey(itemId, materialName, filename))
          resource.files = resource.files ++ Seq(file)

          Item.save(item)

          Ok(toJson(file))
      }
    )


  val SuffixToContentTypes = Map(
    "jpg" -> "image/jpg",
    "jpeg" -> "image/jpg",
    "png" -> "image/png",
    "gif" -> "image/gif",
    "doc" -> "application/msword",
    "pdf" -> "application/pdf")

  private def storageKey(itemId: String, materialName: String, filename: String) = itemId + "/materials/" + materialName + "/" + filename

  private def contentType(filename: String): String = {
    val split = filename.split("\\.").toList
    val suffix = split.last
    SuffixToContentTypes.getOrElse(suffix, "unknown")
  }

  /**
   * check that the item contains a supportingMaterial resource with the supplied name.
   */
  private def canFindResource(resourceName: String)(item: Item): Boolean = item.supportingMaterials.exists(_.name == resourceName)

}
