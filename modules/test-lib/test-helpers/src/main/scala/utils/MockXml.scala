package utils

import scala.xml.transform.{ RuleTransformer, RewriteRule }
import scala.xml.{ Node, Elem, NodeSeq }

object MockXml {

  val AllItems = scala.xml.XML.loadFile("test/mockXml/all-items.xml")

  val incorrectResponseFeedback = "incorrect response feedback"
  val correctResponseFeedback = "correct response feedback"

  def load(filename: String): Elem = scala.xml.XML.loadFile("test/mockXml/%s".format(filename))

  def createXml(identifier: String, cardinality: String, values: NodeSeq, interaction: NodeSeq = <none/>): Elem = {
    <assessmentItem>
      <correctResponseFeedback>{ correctResponseFeedback }</correctResponseFeedback>
      <incorrectResponseFeedback>{ incorrectResponseFeedback }</incorrectResponseFeedback>
      <responseDeclaration identifier={ identifier } cardinality={ cardinality } baseType="string">
        <correctResponse>
          { values }
        </correctResponse>
      </responseDeclaration>
      <itemBody>
        { interaction }
      </itemBody>
    </assessmentItem>
  }

  def removeNodeFromXmlWhere(xml: NodeSeq)(fn: Elem => Boolean): Node = {
    val removeIt = new RewriteRule {
      override def transform(n: Node): NodeSeq = n match {
        case e: Elem if (fn(e)) => NodeSeq.Empty
        case n => n
      }
    }

    new RuleTransformer(removeIt).transform(xml).head
  }

  def replaceNodeInXmlWhere(xml: NodeSeq, newNode: Elem)(fn: Elem => Boolean): Node = {
    val replaceIt = new RewriteRule {
      override def transform(n: Node): NodeSeq = n match {
        case e: Elem if (fn(e)) => newNode
        case n => n
      }
    }

    new RuleTransformer(replaceIt).transform(xml).head
  }

  def addChildNodeInXmlWhere(xml: NodeSeq, newNode: Elem)(fn: Elem => Boolean): Node = {
    val replaceIt = new RewriteRule {
      override def transform(n: Node): NodeSeq = n match {
        case e: Elem if (fn(e)) => e.copy(child = e.child ++ newNode)
        case n => n
      }
    }

    new RuleTransformer(replaceIt).transform(xml).head
  }

}
