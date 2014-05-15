import play.api.mvc.{AnyContent, Controller, BodyParser, Action}
import scala.concurrent.Future

trait ResourceController[R, E] extends Controller {

  def p: BodyParser[E]

  def create: Action[E] = Action.async(p) { r =>

    val model: E = r.body

    val validated = validate(model)

    val permission = getPermission[E](requester, Verb.Create, Some(validated), None)
    if(permission.granted){
      Future(Ok(""))
    } else {
      Future(Unauthorized(permission.reasons.mkString(", ")))
    }
  }

}

trait Resource {}

object Verb extends Enumeration {
  type Verb = Value
  val Create, Read, Update, Delete, Share = Value
}

trait PermissionResult {
  def granted: Boolean

  def reasons: Seq[String]
}

case object PermissionGranted extends PermissionResult {
  override def granted: Boolean = true

  override def reasons: Seq[String] = Seq.empty
}

case class PermissionDenied(val reasons: Seq[String]) extends PermissionResult {
  override def granted: Boolean = false
}

trait PermissionGranter[ASKER, ENTITY] {

  def getPermission(asker: ASKER, verb: Verb, newValue: Option[ENTITY], oldValue: Option[ENTITY]): PermissionResult
}