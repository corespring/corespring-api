package qti.processors

import xml.{XML, NodeSeq, Node, Elem}
import xml.transform.{RuleTransformer, RewriteRule}
import util.matching.Regex

object SelectTextInteractionProcessor extends XmlValidator{

  /**
   * Validate the xml string and return a validation result object
   * @param xmlString
   * @return
   */
  // TODO: implement this
  def validate(xmlString: String): XmlValidationResult = XmlValidationResult.success

  def parseCorrectResponses(selectnodeXml:NodeSeq):Seq[String] = {
    val isWord = (selectnodeXml  \ "@selectionType").text == "word"
    val taggedXml = new RuleTransformer(if (isWord) TagSelectableWords else TagSelectableSentences).transform(selectnodeXml)
    val idRegexp = new Regex("id=\".([0-9]+)", "match")
    val correctIndexes =
      if (isWord) {
        (taggedXml \ "correct").map(n => idRegexp.findFirstMatchIn(n.mkString).get.group("match"))
      } else {
       (taggedXml
          \ "span"
          filterNot (_ \ "@selectable" isEmpty)
          filterNot (_ \ "correct" isEmpty)
        ).map(n => idRegexp.findFirstMatchIn(n.mkString).get.group("match"))
      }
    correctIndexes
  }

  def tokenizeSelectText(qtiXml:NodeSeq):NodeSeq = {
    val isWord = (qtiXml  \\ "selectTextInteraction" \ "@selectionType").text == "word"
    val taggedXml = new RuleTransformer(if (isWord) TagSelectableWords else TagSelectableSentences).transform(qtiXml)
    new RuleTransformer(RemoveCorrectTags).transform(taggedXml)
  }

  def performOnText(e:NodeSeq, fn: String=>String):NodeSeq = {
    val xmlText = e.mkString
    val regExp = "<.*?>".r
    val matches = regExp.findAllIn(xmlText).toList

    val openString = matches.head
    val closeString = regExp.findAllIn(xmlText).toList.last

    val lastIndex = xmlText.lastIndexOf("<")
    val resultText = xmlText.substring(0, lastIndex).replaceFirst("<.*?>","")

    val transformedText = fn(resultText)

    XML.loadString(openString + transformedText + closeString)
  }

  object TagSelectableWords extends RewriteRule {
    def tagWords(s:String):String = {
      val regExp = new Regex("(?<![</&])\\b([a-zA-Z_']+)\\b", "match")
      var idx = 0
      regExp.replaceAllIn(s, m => { idx = idx + 1; "<span selectable=\"\" id=\"s"+idx.toString+"\">"+m.group("match")+"</span>"})
    }

    override def transform(n: Node): Seq[Node] = {
      n match {
        case e:Elem if(e.label == "selectTextInteraction") => performOnText(e, tagWords)
        case _ => n
      }
    }
  }

  object TagSelectableSentences extends RewriteRule {
    def tagSentences(s:String):String = {
      val regExp = new Regex("(?s)(.*?[.!?](<\\/correct>)*)", "match")
      var idx = 0
      val res = regExp.replaceAllIn(s, m => { idx = idx + 1; "<span selectable=\"\" id=\"s"+idx.toString+"\">"+m.group("match")+"</span>"})
      res
    }
    override def transform(n: Node): Seq[Node] = {
      n match {
        case e:Elem if(e.label == "selectTextInteraction") => performOnText(e, tagSentences)
        case _ => n
      }
    }
  }

  object RemoveCorrectTags extends RewriteRule {
    override def transform(n: Node): Seq[Node] = {
      n match {
        case e:Elem if(e.label == "correct") => e.child.head
        case _ => n
      }
    }
  }



}
