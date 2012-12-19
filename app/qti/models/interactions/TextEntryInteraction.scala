package qti.models.interactions

import xml.{NodeSeq, Elem, Node}

case class TextEntryInteraction(responseIdentifier: String, expectedLength: Int, feedbackBlocks: Seq[FeedbackInline]) extends Interaction {
  def getChoice(identifier: String) = None
}

object TextEntryInteraction extends InteractionCompanion[TextEntryInteraction]{
  def apply(node: Node, itemBody:Option[Node]): TextEntryInteraction = {
    val responseIdentifier = Interaction.responseIdentifier(node)
    TextEntryInteraction(
      responseIdentifier = responseIdentifier,
      expectedLength = expectedLength(node),
      feedbackBlocks = itemBody match {
        case Some(node) => feedbackBlocks(node).filter(_.outcomeIdentifier == responseIdentifier)
        case None => throw new RuntimeException("this interaction requires a reference to the outer qti model");
      }
    )
  }
  def parse(itemBody:Node):Seq[Interaction] = {
    val interactions = (itemBody \ "textEntryInteraction")
    if (interactions.isEmpty){
      Seq()
    }else{
      interactions.map(node => TextEntryInteraction(node,Some(itemBody)))
    }
  }
  private def feedbackBlocks(itemBody:Node):Seq[FeedbackInline] = {
    (itemBody \ "feedbackBlock").map(node => FeedbackInline(node,None))
  }
  private def expectedLength(n: Node): Int = (n \ "@expectedLength").text.toInt

  def interactionMatch(e:Elem):Boolean = e.label == "textEntryInteraction"
}
