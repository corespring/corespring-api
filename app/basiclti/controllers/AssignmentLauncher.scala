package basiclti.controllers

import play.api.mvc.{AnyContent, Request, Action, Controller}
import play.Logger
import basiclti.models.{LtiData, Assignment, LtiOAuthConsumer}
import play.api.libs.ws.WS
import play.api.libs.oauth.{RequestToken, ConsumerKey, OAuthCalculator}
import org.bson.types.ObjectId
import com.mongodb.casbah.commons.MongoDBObject
import models.ItemSession
import play.api.libs.json.Json._

import testplayer.controllers.routes.{ ItemPlayer => ItemPlayerRoutes }
import basiclti.controllers.routes.{ AssignmentPlayer => AssignmentPlayerRoutes }

object AssignmentLauncher extends Controller {

  private def tokenize(url:String,token:String) = "%s?access_token=%s".format(url,token)

  def launch(itemId:ObjectId) = Action{ request =>

    //TODO: Token access?

    LtiData(request) match {
      case Some(d) => {

        require(d.outcomeUrl.isDefined, "no outcome url is defined")
        require(d.resultSourcedId.isDefined, "sourcedid is defined")

        Assignment.findOne( MongoDBObject("resultSourcedId" -> d.resultSourcedId )) match {
          case Some(assignment) => {
            val call = AssignmentPlayerRoutes.runByAssignmentId(assignment.id)
            Redirect( tokenize(call.url, common.mock.MockToken) )
              .withSession(("assignment_id",assignment.id.toString))
          }
          case _ => {
            val newSession = new ItemSession( itemId = itemId )
            ItemSession.save(newSession)
            val newAssignment = new Assignment(
              itemSessionId = newSession.id,
              resultSourcedId = d.resultSourcedId.get,
              gradePassbackUrl = d.outcomeUrl.get,
              onFinishedUrl = d.returnUrl.get
            )
            Assignment.save(newAssignment)

            val call = AssignmentPlayerRoutes.runByAssignmentId(newAssignment.id)

            //TODO: shouldn't be using sessions
            Redirect( tokenize(call.url, common.mock.MockToken ) )
              .withSession(("assignment_id",newAssignment.id.toString))
          }
        }
      }
      case _ => {
        Ok("you're a teacher - in this mode you'll just play with the assessment but there's no marks")
        val testPlayerCall = ItemPlayerRoutes.previewItem(itemId.toString)
        Redirect( tokenize(testPlayerCall.url, common.mock.MockToken))
      }
    }
  }

  def responseXml(sourcedId:String,score:String) = <imsx_POXEnvelopeRequest>
    <imsx_POXHeader>
      <imsx_POXRequestHeaderInfo>
        <imsx_version>V1.0</imsx_version>
        <imsx_messageIdentifier>12341234</imsx_messageIdentifier>
      </imsx_POXRequestHeaderInfo>
    </imsx_POXHeader>
    <imsx_POXBody>
      <replaceResultRequest>
        <resultRecord>
          <sourcedGUID>
            <sourcedId>{sourcedId}</sourcedId>
          </sourcedGUID>
          <result>
            <resultScore>
              <language>en</language>
              <textString>{score}</textString>
            </resultScore>
          </result>
        </resultRecord>
      </replaceResultRequest>
    </imsx_POXBody>
  </imsx_POXEnvelopeRequest>

  def process(assignmentId:ObjectId) = Action{ request =>

     Assignment.findOneById(assignmentId) match {
       case Some(assignment) => {
        ItemSession.findOneById(assignment.itemSessionId) match {
          case Some(session) => {

              val consumer = new LtiOAuthConsumer("1234", "secret")

              val call = WS.url(assignment.gradePassbackUrl)
                .sign(
                OAuthCalculator(ConsumerKey("1234","secret"),
                  RequestToken(consumer.getToken, consumer.getTokenSecret)))
                .withHeaders(("Content-Type", "application/xml"))
                .post(responseXml(assignment.resultSourcedId, "0.3"))

              call.await(10000).fold(
                error => throw new RuntimeException( error.getMessage ) ,
                response => Ok(toJson(Map("returnUrl" -> assignment.onFinishedUrl)))
              )
          }
          case _ => BadRequest("no item session found")
        }
       }
       case _ =>  BadRequest("no assignment found")
     }

  }
}
