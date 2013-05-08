package tests.player.views

import org.specs2.mutable.Specification
import player.views.models.PlayerParams
import play.api.Logger
import tests.PlaySingleton
import scala.xml.Node

class PlayerHtmlTest extends Specification{

  PlaySingleton.start()

  private val logger = Logger(this.getClass.getCanonicalName)

  def xml(mode:String, body:Node) : String = {
    <assessmentItem mode={mode}>
      <itemBody>{body}</itemBody>
    </assessmentItem>.toString()
  }

  def playerHtml(mode:String,body:Node) : String = {
    val html = player.views.html.Player(PlayerParams( xml(mode,body), Some("?"), Some("?") ))
    html.body
  }

  "player html" should {

    "render" in {
      val n = <choiceInteraction></choiceInteraction>
      playerHtml("Web", n).contains("/web/choiceInteraction.css") === true
      playerHtml("Web", n).contains("/web/simpleChoice.css") === true
      playerHtml("Aggregate", n).contains("/aggregate/simpleChoice.css") === true
      playerHtml("Printing", n).contains("/printing/simpleChoice.css") === true
      playerHtml("Printing", n).contains("/printing/choiceInteraction.css") === true
      //This css doesn't exist so shouldn't be in the html
      playerHtml("Instructor", n).contains("/instructor/simpleChoice.css") === false
      playerHtml("Instructor", n).contains("/instructor/choiceInteraction.css") === false
    }
  }

}
