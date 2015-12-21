package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.models.item.Passage
import org.corespring.models.item.resource.VirtualFile
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.PassageService
import org.corespring.v2.auth.PassageAuth
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.V2Error
import play.api.mvc.{SimpleResult, RequestHeader}

import scala.concurrent.{Future, ExecutionContext}
import scalaz.{Failure, Success, Validation}

class PassageApi(
  passageAuth: PassageAuth,
  v2ApiContext: V2ApiExecutionContext,
  override val getOrgAndOptionsFn: RequestHeader => Validation[V2Error, OrgAndOpts]) extends V2Api {

  override implicit def ec: ExecutionContext = v2ApiContext.context

  def get(passageId: VersionedId[ObjectId], itemId: Option[String]) = futureWithIdentity { (identity, request) =>
    Future.successful(passageAuth.loadForRead(passageId.toString, itemId.map(VersionedId(_)).flatten)(identity) match {
      case Success(passage) => PassageResponseWriter.write(passage)
      case Failure(error) => Status(error.statusCode)(error.message)
    })
  }

  private object PassageResponseWriter {

    def write(passage: Passage): SimpleResult = passage.file match {
      case file: VirtualFile => Ok(file.content).withHeaders("Content-Type" -> file.contentType)
      case _ => throw new UnsupportedOperationException("Cannot support files other than VirtualFile at the moment")
    }

  }

}
