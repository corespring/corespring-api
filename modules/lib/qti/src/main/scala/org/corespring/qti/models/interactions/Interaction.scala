package org.corespring.qti.models.interactions

import org.corespring.qti.models.responses._
import org.corespring.qti.models._
import org.corespring.common.log.PackageLogging
import play.api.libs.json.JsValue
import xml._
import org.corespring.qti.models.responses.javascript.DefaultResponseConverter
import com.scalapeno.rhinos.JavascriptProcessor

trait Interaction extends PackageLogging with JavascriptProcessor {
  val responseIdentifier: String
  protected val locked: Boolean = false;
  protected val nonInteractive:Boolean = false;

  def getOutcome(responseDeclaration: Option[ResponseDeclaration], response: Response): Option[ResponseOutcome]

  /**
   * Can this Interaction be automatically scored from the users response
   * Eg: multichoice can - but free written text can't be
   * @return
   */
  def isScoreable: Boolean

  def validate(qtiItem: QtiItem): (Boolean, String) = {
    val isValid = !qtiItem.responseDeclarations.find(_.identifier == responseIdentifier).isEmpty || locked || nonInteractive
    val msg = if (isValid) "Ok" else "Missing response declaration for " + responseIdentifier
    (isValid, msg)
  }

  def toJs(response: Response): Option[(String,JsValue)] = None
  def jsPreprocessor: String = ""
}

trait InteractionCompanion[T <: Interaction] {
  def tagName: String
  def apply(interaction: Node, itemBody: Option[Node]): T
  def parse(itemBody: Node): Seq[Interaction]
  def interactionMatch(e: Elem): Boolean = e.label == tagName
  def preProcessXml(interactionXml: Elem): NodeSeq = interactionXml
}

object Interaction {
  def responseIdentifier(n: Node) = (n \ "@responseIdentifier").text
}
