package org.corespring.api.v1

import org.bson.types.ObjectId
import org.corespring.amazon.s3.S3Service
import org.corespring.common.config.AppConfig
import org.corespring.models.auth.Permission
import org.corespring.models.item.Item
import org.corespring.models.item.resource.{ VirtualFile, BaseFile, StoredFile, Resource }
import org.corespring.models.json.JsonFormatting
import org.corespring.platform.core.controllers.auth.{ ApiRequest, BaseApi }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.services.ContentCollectionService
import org.corespring.services.item.ItemService
import org.corespring.v2.sessiondb.{ SessionServices, SessionService }
import org.corespring.web.api.v1.errors.ApiError
import play.api.Logger
import play.api.libs.json.Json._
import play.api.libs.json.{ JsArray, JsObject, JsString, _ }
import play.api.mvc._

class ResourceApi(
  s3service: S3Service,
  itemTransformer: ItemTransformer,
  itemService: ItemService,
  contentCollectionService: ContentCollectionService,
  jsonFormatting: JsonFormatting,
  sessionServices: SessionServices)
  extends BaseApi {

  import jsonFormatting._

  override lazy val logger = Logger(classOf[ResourceApi])

  private val USE_ITEM_DATA_KEY: String = "__!data!__"

  val DATA_PATH: String = "data"

  /**
   * Item.data has at least one file that is always default its name is set here.
   * TODO: move this elsewhere
   */
  private val DEFAULT_DATA_FILE_NAME: String = Item.QtiResource.QtiXml

  /**
   * A class that adds an AuthorizationContext to the Request object
   * @param item - the found item
   * @param r - the Request
   * @tparam A - the type determining the type of the body parser (eg: AnyContent)
   */
  case class ItemRequest[A](item: Item, r: Request[A]) extends WrappedRequest(r)

  /**
   * TODO: This is not working as expected - needs a rewrite.
   * See: https://gist.github.com/edeustace/641e35e9d40e8dec7d6a
   * As an alternative approach
   * Because ApiAction(p) is passing the bodyparser straight through -aka- it gets run first.
   * A wrapping Action that checks that an Item with the given id exists before executing the action body.
   * @param itemId - the item id
   * @param additionalChecks - a sequence of additional checks beyond checking for the item.
   * @param action - the action to invoke
   * @tparam A
   */
  def HasItem[A](
    itemId: String,
    additionalChecks: Seq[(ApiRequest[A], Item) => Option[Result]],
    p: BodyParser[A])(
      action: ItemRequest[A] => Result) = ApiAction(p) { request =>
    convertVersionedId(itemId) match {
      case Some(validId) => {
        itemService.findOneById(validId) match {
          case Some(item) => {
            val errors: Seq[Result] = additionalChecks.flatMap(_(request, item))
            if (errors.length == 0) {
              action(ItemRequest(item, request))
            } else {
              //TODO: Only returning the first error
              errors(0)
            }
          }
          case _ => NotFound
        }
      }
      case _ => NotFound
    }
  }

  /**
   * @param itemId
   * @return an Option[ObjectId] or None if the id is invalid
   */
  private def convertVersionedId(itemId: String): Option[VersionedId[ObjectId]] = {
    logger.debug("handle itemId: " + itemId)
    VersionedId(itemId)
  }

  def HasItem(itemId: String,
    additionalChecks: Seq[(ApiRequest[AnyContent], Item) => Option[Result]] = Seq(),
    action: ItemRequest[AnyContent] => Result): Action[AnyContent] = HasItem(itemId, additionalChecks, parse.anyContent)(action)

  private def removeFileFromResource(resource: Resource, filename: String)(putResourceInItem: Resource => Item): Result = {
    resource.files.find(_.name == filename) match {
      case Some(f) => {

        val updated = resource.copy(files = resource.files.filterNot(_.name == filename))
        val updatedItem = putResourceInItem(updated)
        f match {
          case StoredFile(_, _, _, key) => s3service.delete(AppConfig.assetsBucket, key)
          case _ => //do nothing
        }
        itemService.save(updatedItem)
        Ok
      }
      case _ => NotFound(filename)
    }
  }

  def editCheck(force: Boolean = false) = new Function2[ApiRequest[_], Item, Option[Result]] {
    def apply(request: ApiRequest[_], item: Item): Option[Result] = {
      if (contentCollectionService.isAuthorized(request.ctx.orgId, new ObjectId(item.collectionId), Permission.Write)) {
        if (sessionServices.main.sessionCount(item.id) > 0 && item.published && !force) {
          Some(Forbidden(toJson(JsObject(Seq("message" ->
            JsString("Action cancelled. You are attempting to change an item's content that contains session data. You may force the change by appending force=true to the url, but you will invalidate the corresponding session data. It is recommended that you increment the revision of the item before changing it"),
            "flags" -> JsArray(Seq(JsString("alert_increment"))))))))
        } else {
          None
        }
      } else Some(Unauthorized(toJson(ApiError.UnauthorizedOrganization)))
    }
  }

  private def swapInUpdate(update: Resource)(r: Resource) = {
    if (r.name == update.name) {
      update
    } else {
      r
    }
  }

  def deleteSupportingMaterialFile(itemId: String, resourceName: String, filename: String) = HasItem(
    itemId,
    Seq(editCheck()),
    {
      request: ItemRequest[AnyContent] =>
        val item = request.item //.asInstanceOf[ItemRequest[AnyContent]].item
        item.supportingMaterials.find(_.name == resourceName) match {
          case Some(r) => {
            removeFileFromResource(r, filename)((update) => {
              item.copy(supportingMaterials = item.supportingMaterials.map(swapInUpdate(update)))
            })
          }
          case _ => NotFound(resourceName)
        }
    })

  def deleteDataFile(itemId: String, filename: String, force: Boolean) = HasItem(
    itemId,
    Seq(editCheck(force)),
    {
      request: ItemRequest[AnyContent] =>
        val item = request.item
        if (filename == DEFAULT_DATA_FILE_NAME) {
          BadRequest("Can't delete " + DEFAULT_DATA_FILE_NAME)
        } else {
          removeFileFromResource(item.data.get, filename)((update) => {
            item.copy(data = Some(update))
          })
        }
    })

  def createSupportingMaterialFile(itemId: String, resourceName: String) = HasItem(
    itemId,
    Seq(editCheck()),
    {
      request: ItemRequest[AnyContent] =>
        request.body.asJson match {
          case Some(json) => {
            val item = request.item
            item.supportingMaterials.find(_.name == resourceName) match {
              case Some(r) => {
                json.asOpt[BaseFile] match {
                  case Some(file) => {
                    saveFileIfNameNotTaken(r, file)((update: Resource) => {

                      def updateResource(r: Resource) = {
                        if (r.name == update.name) {
                          update
                        } else {
                          r
                        }
                      }

                      item.copy(supportingMaterials = item.supportingMaterials.map(updateResource))
                    })
                  }
                  case _ => BadRequest
                }
              }
              case _ => NotFound
            }
          }
          case _ => BadRequest
        }
    })

  def createDataFile(itemId: String) = HasItem(
    itemId,
    Seq(editCheck()),
    {
      request: ItemRequest[AnyContent] =>
        request.body.asJson match {
          case Some(json) => {
            val item = request.item
            json.asOpt[BaseFile] match {
              case Some(file) => {
                if (!item.data.isDefined) {
                  throw new RuntimeException("item.data should never be undefined")
                }

                val processedFile = ensureDataFileIsMainIsCorrect(file)
                saveFileIfNameNotTaken(item.data.get, processedFile)((update: Resource) => {
                  item.copy(data = Some(update))
                })
              }
              case _ => BadRequest
            }
          }
          case _ => BadRequest
        }
    })

  private def ensureDataFileIsMainIsCorrect(file: BaseFile): BaseFile = {
    val isMain = file.name == DEFAULT_DATA_FILE_NAME
    copyFile(file, Some(isMain))
  }

  /**
   * Copy the file and okoptionally override the isMain attribute
   * @param file
   * @param enforceIsMain
   * @return
   */
  private def copyFile(file: BaseFile, enforceIsMain: Option[Boolean]): BaseFile = {

    def copy(file: BaseFile, isMain: Boolean): BaseFile = file match {
      case VirtualFile(name, contentType, _, contents) => VirtualFile(name, contentType, isMain, contents)
      case StoredFile(name, contentType, _, key) => StoredFile(name, contentType, isMain, key)
      case _ => throw new RuntimeException("Unknown file type")
    }

    enforceIsMain match {
      case Some(b) => copy(file, b)
      case _ => copy(file, file.isMain)
    }
  }

  private def updateKey(update: BaseFile, file: BaseFile): BaseFile = {
    if (file.name == update.name && file.isInstanceOf[StoredFile]) {
      update.asInstanceOf[StoredFile].copy(storageKey = file.asInstanceOf[StoredFile].storageKey)
    } else {
      file
    }
  }

  def updateDataFile(itemId: String, filename: String, force: Boolean) = HasItem(
    itemId,
    Seq(editCheck(force)),
    {
      request: ItemRequest[AnyContent] =>
        getFileFromJson(request.body) match {
          case Some(update) => {
            val item = request.item
            item.data.get.files.find(_.name == filename) match {
              case Some(f) => {
                val processedUpdate = ensureDataFileIsMainIsCorrect(update)
                val updatedData = item.data.map(d => d.copy(files = d.files.map(updateKey(update, _))))
                val i = item.copy(data = updatedData)
                itemTransformer.updateV2Json(i)
                Ok(toJson(processedUpdate))
              }
              case _ => NotFound(update.name)
            }
          }
          case _ => BadRequest
        }
    })

  private def getFileFromJson(body: AnyContent): Option[BaseFile] = {
    body.asJson match {
      case Some(json) => {
        val file = json.asOpt[BaseFile]
        file
      }
      case _ => None
    }
  }

  def updateSupportingMaterialFile(itemId: String, resourceName: String, filename: String) = HasItem(
    itemId,
    Seq(editCheck()),
    {
      request: ItemRequest[AnyContent] =>
        getFileFromJson(request.body) match {
          case Some(update) => {
            val item = request.item
            item.supportingMaterials.find(_.name == resourceName) match {
              case Some(resource) => {
                resource.files.find(_.name == filename) match {
                  case Some(f) => {

                    val updateAndUnset: BaseFile => BaseFile = {
                      val f: BaseFile => BaseFile = updateKey(update, _)
                      f.andThen(unsetIsMain _)
                    }

                    def updateFile(f: BaseFile): BaseFile = {
                      val nameMatches = (f.name == update.name)

                      (nameMatches, update.isMain) match {
                        case (true, true) => updateAndUnset(f)
                        case (false, true) => unsetIsMain(f)
                        case (true, false) => updateKey(update, f)
                        case (false, false) => f
                      }
                    }

                    def updateResource(r: Resource): Resource = {
                      r.copy(id = r.id, name = r.name, materialType = r.materialType, files = r.files.map(updateFile))
                    }

                    val updatedSupportingMaterials = item.supportingMaterials.map(updateResource)
                    val i = item.copy(supportingMaterials = updatedSupportingMaterials)
                    itemService.save(i)
                    Ok(toJson(i)(jsonFormatting.item))
                  }
                  case _ => NotFound
                }
              }
              case _ => NotFound(resourceName)
            }
          }
          case _ => BadRequest
        }
    })

  /**
   * Create the storage key for the resource.
   * The key must have the format:
   * - data resources: $itemId/$version/data/$filename
   * - supporting materials: $itemId/$version/materials/$resourceName/$filename
   *
   * It finds the actual versionedId stored in the db - to ensure that the version is part of the storageKey.
   * @param itemId - a string representation of the versioned id
   * @param keys - the subfolders
   */
  private def key(itemId: String, keys: String*): String = {

    val someItem: Option[Item] = for {
      vId <- convertVersionedId(itemId)
      item <- itemService.findOneById(vId)
    } yield item

    someItem.map { i =>
      require(i.id.version.isDefined, "The version must be defined")
      (Seq(i.id.id.toString, i.id.version.get) ++ keys).mkString("/")
    }.getOrElse(throw new RuntimeException("Can't find item by id: " + itemId))

  }

  /**
   * Upload a file to the 'data' Resource in the Item.
   * @param itemId
   * @param filename
   * @return
   */
  def uploadFileToData(itemId: String, filename: String) = {
    HasItem(
      itemId,
      Seq(editCheck(), isFilenameTaken(filename, USE_ITEM_DATA_KEY)(_, _)),
      s3service.upload(AppConfig.assetsBucket, key(itemId, DATA_PATH, filename)))(
        {
          request =>

            def getOrCreateData(i: Item) = i.data.getOrElse(Resource(name = "data", files = Seq.empty))
            val item = request.item

            val resource = getOrCreateData(item)

            val file = new StoredFile(
              filename,
              contentType(filename),
              false,
              key(itemId, DATA_PATH, filename))

            val updated = resource.copy(files = resource.files ++ Seq(file))
            val i = item.copy(data = Some(updated))
            itemService.save(i)
            Ok(toJson(file))
        })
  }

  /**
   * Upload a file to a supporting material Resource in the item.
   * @param itemId
   * @param materialName
   * @param filename
   * @return
   */
  def uploadFile(itemId: String, materialName: String, filename: String) =
    HasItem(itemId,
      Seq(editCheck(),
        canFindResource(materialName)(_, _),
        isFilenameTaken(filename, materialName)(_, _)),
      s3service.upload(AppConfig.assetsBucket, storageKey(itemId, materialName, filename)))(
        {
          request =>
            val item = request.asInstanceOf[ItemRequest[AnyContent]].item

            val file = new StoredFile(
              filename,
              contentType(filename),
              false,
              storageKey(itemId, materialName, filename))

            def updateResource(r: Resource): Resource = {
              if (r.name == materialName) {
                r.copy(files = r.files :+ file)
              } else {
                r
              }
            }

            val update = item.copy(supportingMaterials = item.supportingMaterials.map(updateResource))
            itemService.save(update)
            Ok(toJson(file))
        })

  def getSupportingMaterials(itemId: String) = HasItem(itemId, Seq(), {
    request =>
      Ok(toJson(request.item.supportingMaterials))
  })

  def createSupportingMaterialWithFile(itemId: String, name: String, filename: String) = {
    val s3Key = storageKey(itemId, name, filename)
    HasItem(itemId, Seq(editCheck()), s3service.upload(AppConfig.assetsBucket, s3Key))(
      {
        request =>
          val item = request.asInstanceOf[ItemRequest[AnyContent]].item
          item.supportingMaterials.find(_.name == name) match {
            case Some(foundResource) => NotAcceptable(toJson(ApiError.ResourceNameTaken))
            case _ => {
              val file = new StoredFile(filename, contentType(filename), true, s3Key)
              val resource = Resource(name = name, files = Seq(file))
              val update = item.copy(supportingMaterials = item.supportingMaterials ++ Seq(resource))
              itemService.save(update)
              Ok(toJson(resource))
            }
          }
      })
  }

  def createSupportingMaterial(itemId: String) = HasItem(itemId, Seq(editCheck()), { request: ItemRequest[AnyContent] =>

    request.body.asJson match {
      case Some(json) => {
        json.asOpt[Resource] match {
          case Some(foundResource) => {
            isResourceNameTaken(foundResource.name)(request.item) match {
              case Some(error) => NotAcceptable(toJson(error))
              case _ => {
                val update = request.item.copy(supportingMaterials = request.item.supportingMaterials ++ Seq[Resource](foundResource))
                itemService.save(update)
                Ok(toJson(foundResource))
              }
            }
          }
          case _ => BadRequest(Json.toJson(ApiError.JsonExpected))
        }
      }
      case _ => BadRequest(Json.toJson(ApiError.JsonExpected))
    }
  })

  def deleteSupportingMaterial(itemId: String, resourceName: String) = HasItem(itemId,
    Seq(editCheck(), canFindResource(resourceName)(_, _)),
    {
      request: ItemRequest[AnyContent] =>
        val update = request.item.copy(supportingMaterials = request.item.supportingMaterials.filterNot(_.name == resourceName))
        itemService.save(update)
        Ok("")
    })

  private def unsetIsMain(f: BaseFile) = {
    f match {
      case VirtualFile(n, ct, _, k) => VirtualFile(n, ct, false, k)
      case StoredFile(n, ct, _, c) => StoredFile(n, ct, false, c)
    }
  }

  private def unsetIsMain(resource: Resource): Resource = {
    resource.copy(files = resource.files.map(unsetIsMain))
  }

  private def saveFileIfNameNotTaken(resource: Resource, file: BaseFile)(putResourceInItem: Resource => Item): Result =
    resource.files.find(_.name == file.name) match {
      case Some(existingFile) => NotAcceptable(toJson(ApiError.FilenameTaken(Some(file.name))))
      case _ => {
        val r = if (file.isMain) unsetIsMain(resource) else resource
        val o = r.copy(files = r.files :+ file)
        val i = putResourceInItem(o)
        itemService.save(i)
        Ok(toJson(file))
      }
    }

  private def storageKey(itemId: String, materialName: String, filename: String) = key(itemId, "materials", materialName, filename)

  private def contentType(filename: String): String = BaseFile.getContentType(filename)

  /**
   * check that the item contains a supportingMaterial resource with the supplied name.
   */
  private def canFindResource(resourceName: String)(request: ApiRequest[_], item: Item): Option[Result] = {
    if (item.supportingMaterials.exists(_.name == resourceName)) {
      None
    } else {
      Some(NotFound(toJson(ApiError.ResourceNotFound(Some(resourceName)))))
    }
  }

  private def getResource(item: Item, resourceName: String): Option[Resource] = {
    if (resourceName == USE_ITEM_DATA_KEY) {
      item.data
    } else {
      item.supportingMaterials.find(_.name == resourceName)
    }
  }

  private def isResourceNameTaken(resourceName: String)(item: Item): Option[ApiError] = {
    getResource(item, resourceName) match {
      case Some(r) => Some(ApiError.ResourceNameTaken(Some(resourceName)))
      case _ => None
    }
  }

  private def isFilenameTaken(filename: String, resourceName: String)(request: ApiRequest[_], item: Item): Option[Result] = {

    getResource(item, resourceName) match {
      case Some(r) => {
        if (r.files.exists(_.name == filename)) {
          Some(NotFound(toJson(ApiError.FilenameTaken(Some(filename)))))
        } else {
          None
        }
      }
      case _ => None
    }
  }
}

