package qti.models.interactions

import choices.{Choice, SimpleChoice}

/**
 * Created with IntelliJ IDEA.
 * User: josh
 * Date: 12/18/12
 * Time: 1:37 PM
 * To change this template use File | Settings | File Templates.
 */
trait InteractionWithChoices extends Interaction{
  val choices: Seq[Choice]
  def getChoice(identifier: String):Option[Choice];
}
