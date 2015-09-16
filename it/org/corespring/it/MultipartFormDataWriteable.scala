package org.corespring.it

import java.io.ByteArrayOutputStream

import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.content.{ FileBody, StringBody }
import play.api.http.Writeable
import play.api.libs.Files.TemporaryFile
import play.api.mvc.{ AnyContentAsMultipartFormData, Codec, MultipartFormData }

object MultipartFormDataWriteable {

  implicit def writeableOf_multipartFormData(implicit codec: Codec): Writeable[AnyContentAsMultipartFormData] = {

    val entity = new MultipartEntity()

    def toBytes(multipart: MultipartFormData[TemporaryFile]): Array[Byte] = {

      multipart.dataParts.foreach { part =>
        part._2.foreach { p2 =>
          entity.addPart(part._1, new StringBody(p2))
        }
      }

      multipart.files.foreach { file =>
        val part = new FileBody(file.ref.file, file.filename, file.contentType.getOrElse("application/octet-stream"), null)
        entity.addPart(file.key, part)
      }

      val outputStream = new ByteArrayOutputStream
      entity.writeTo(outputStream)
      val bytes = outputStream.toByteArray
      outputStream.close
      bytes
    }

    def transform(m: AnyContentAsMultipartFormData): Array[Byte] = {
      toBytes(m.mdf)
    }

    new Writeable[AnyContentAsMultipartFormData](transform, Some(entity.getContentType.getValue))
  }

}
