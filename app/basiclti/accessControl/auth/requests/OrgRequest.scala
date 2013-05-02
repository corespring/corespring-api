package basiclti.accessControl.auth.requests

import org.bson.types.ObjectId
import play.api.mvc.{WrappedRequest, Request}


case class OrgRequest[A](orgId:ObjectId, r:Request[A]) extends WrappedRequest[A](r)
