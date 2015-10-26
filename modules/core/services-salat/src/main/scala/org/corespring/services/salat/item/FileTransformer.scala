package org.corespring.services.salat.item

import com.novus.salat.transformers.CustomTransformer
import grizzled.slf4j.Logger
import com.mongodb.casbah.Imports._
import org.corespring.models.item.resource.{ StoredFile, VirtualFile, BaseFile }

class FileTransformer extends CustomTransformer[BaseFile, DBObject] {

  lazy val logger = Logger(classOf[FileTransformer])

  override def deserialize(b: DBObject): BaseFile = {

    for {
      name <- b.expand[String]("name")
      contentType <- b.expand[String]("contentType")
      isMain <- b.expand[Boolean]("isMain")
    } yield {

      val maybeContent = b.expand[String]("content")

      maybeContent match {
        case Some(c) => VirtualFile(name, contentType, isMain, c)
        case _ => {
          //V1 Support for storageKey
          val storageKey = b.expand[String]("storageKey").getOrElse("")
          StoredFile(name, contentType, isMain, storageKey)
        }
      }
    }
  }.getOrElse {
    throw new RuntimeException(s"Can't convert dbo into BaseFile: $b")
  }

  override def serialize(a: BaseFile): DBObject = {
    logger.trace(s"function=serialize, a=$a")
    val builder = MongoDBObject.newBuilder

    builder += ("name" -> a.name)
    builder += ("contentType" -> a.contentType)
    builder += ("isMain" -> a.isMain)

    a match {
      case VirtualFile(_, _, _, content) => builder += ("content" -> content)
      case StoredFile(_, _, _, storageKey) => builder += ("storageKey" -> storageKey)
    }

    val out = builder.result
    logger.trace(s"function=serialize, out=$out")
    out
  }
}
