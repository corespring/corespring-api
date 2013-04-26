package tests.player.accessControl.auth

import player.accessControl.models.{RequestedAccess, RenderOptions}
import org.bson.types.ObjectId
import org.specs2.mutable.Specification
import player.accessControl.auth.CheckPlayerSession

class CheckPlayerSessionTest extends Specification {


  val assessmentId = "000000000000000000000001"
  val itemId = "5153eee1aa2eefdc1b7a5570"
  val sessionId = "5153effbaa2eefdc1b7a5571"
  val mode = "render"
  val ro1 = RenderOptions(itemId,"*","*","student",0,mode)
  val ro2 = RenderOptions("*","*",assessmentId,"student",0,mode)
  val ra1 = RequestedAccess(Some(new ObjectId(itemId)))
  val ra2 = RequestedAccess(Some(new ObjectId("50b653a1e4b0ec03f29344b0")))
  "BaseRender.hasAccess" should {
    "return true when requested item is same as item in options" in {
      CheckPlayerSession.grantAccess(Some("preview"), ra1,ro1) must beRight
    }
    "return error when requested item is not the same as item in options" in {
      CheckPlayerSession.grantAccess(Some("preview"),ra2,ro1) must beLeft
    }
    "return true when requested item is contained in options assessment" in {
      CheckPlayerSession.grantAccess(Some("preview"),ra1,ro2) must beRight
    }
    "return error when requested item is not contained in options assessment" in {
      CheckPlayerSession.grantAccess(Some("preview"),ra2,ro2) must beLeft
    }
  }

}
