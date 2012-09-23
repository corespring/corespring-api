package controllers.testplayer.qti

import org.specs2.mutable._

class QtiItemSpec extends Specification {
  class FeedbackSequenceMatcher(feedback: Seq[FeedbackElement]) {
    def matches(ids: List[Int]): Boolean = feedback.map(_.csFeedbackId.toInt).toSet equals ids.toSet
  }

  implicit def feedbackSeqToMatcher(feedback: Seq[FeedbackElement]) = new FeedbackSequenceMatcher(feedback)

  "A mutiple choice item" should {


    val correctFeedbackIds = List[Int](1,2)
    val incorrectFeedbackIds = List[Int](3,4,5,6)

    val xml = <assessmentItem
    identifier="mcas-16373" timeDependent="false" title="16373" xsi:schemaLocation="http://www.imsglobal.org/xsd/imsqti_v2p1 imsqti_v2p1.xsd" toolName="SIB" adaptive="false" toolVersion="1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.imsglobal.org/xsd/imsqti_v2p1">
      <stylesheet type="text/css" href="css/mcas/default.css"></stylesheet>

      <!-- single choice -->
      <responseDeclaration identifier="mexicanPresident" cardinality="single" baseType="identifier">
        <correctResponse>
          <value>calderon</value>
        </correctResponse>
      </responseDeclaration>

      <!-- single choice -->
      <responseDeclaration identifier="irishPresident" cardinality="single" baseType="identifier">
        <correctResponse>
          <value>higgins</value>
        </correctResponse>
      </responseDeclaration>

      <!-- multi choice -->
      <responseDeclaration identifier="rainbowColors" cardinality="multiple" baseType="identifier">
        <correctResponse>
          <value>blue</value>
          <value>violet</value>
          <value>red</value>
        </correctResponse>
        <!-- example of mapping score -->
        <mapping lowerBound="0" upperBound="3" defaultValue="-3">
          <mapEntry mapKey="blue" mappedValue="1"/>
          <mapEntry mapKey="violet" mappedValue="1"/>
          <mapEntry mapKey="red" mappedValue="1"/>
          <mapEntry mapKey="white" mappedValue="-3"/>
        </mapping>
      </responseDeclaration>

      <!-- inline text interaction -->
      <responseDeclaration identifier="winterDiscontent" cardinality="single" baseType="string">
        <correctResponse>
          <value>York</value>
        </correctResponse>
        <mapping defaultValue="0">
          <mapEntry mapKey="York" mappedValue="1"/>
          <mapEntry mapKey="york" mappedValue="0.5"/>
        </mapping>
      </responseDeclaration>

      <!-- orderInteraction -->
      <responseDeclaration identifier="wivesOfHenry" cardinality="ordered" baseType="identifier">
        <correctResponse>
          <value>aragon</value>
          <value>boleyn</value>
          <value>seymour</value>
          <value>cleves</value>
          <value>howard</value>
          <value>parr</value>
        </correctResponse>
      </responseDeclaration>


      <!-- orderInteraction -->
      <responseDeclaration identifier="cutePugs" cardinality="ordered" baseType="identifier">
        <correctResponse>
          <value>pug1</value>
          <value>pug2</value>
          <value>pug3</value>
        </correctResponse>
      </responseDeclaration>

      <!-- extended text interaction -->
      <responseDeclaration identifier="longAnswer" cardinality="single" baseType="string"/>

      <itemBody>
        <choiceInteraction responseIdentifier="mexicanPresident" shuffle="false" maxChoices="1">
          <prompt>Who is the President of Mexico?</prompt>
          <simpleChoice identifier="obama"> Barack Obama
            <feedbackInline showHide="show">Incorrect, Barack Obama is President of the USA</feedbackInline>
            <!-- feedback will be parsed and converted to format like this
                <feedbackInline csFeedbackId="bush" outcomeIdentifier="responses.mexicanPresident.value" identifier="bush" showHide="show"></feedbackInline>
                -->
          </simpleChoice>
          <simpleChoice identifier="cameron"> James Cameron
            <feedbackInline showHide="show">Incorrect, James Cameron is Prime Minister of the UK</feedbackInline>
          </simpleChoice>
          <simpleChoice identifier="calderon"> Felipe Calderon
            <feedbackInline showHide="show"><b>Correct!</b>, Felipe Calderon is the President of Mexico</feedbackInline>
          </simpleChoice>
          <simpleChoice identifier="netanyahu"> Benjamin Netanyahu
            <feedbackInline showHide="show">Incorrect, Benjamin Netanyahu is the Prime Minister of Israel</feedbackInline>
          </simpleChoice>
        </choiceInteraction>

        <!-- this one has feedback pre-populated with csFeedbackId, outcomeIdentifier and identifier -->
        <choiceInteraction responseIdentifier="irishPresident" shuffle="false" maxChoices="1">
          <prompt>Who is the President of Ireland?</prompt>
          <simpleChoice identifier="robinson"> Mary Robinson
            <feedbackInline csFeedbackId="fbRobinson" outcomeIdentifier="responses.irishPresident.value" identifier="robinson" showHide="show">Incorrect, Mary Robinson was President of Ireland from 1990 to 1997</feedbackInline>
          </simpleChoice>
          <simpleChoice identifier="higgins"> Michel D. Higgins
            <feedbackInline csFeedbackId="fbHiggins" outcomeIdentifier="responses.irishPresident.value" identifier="higgins" showHide="show"><b>Correct</b>, Michael D. Higgins is the sitting President of Ireland</feedbackInline>
          </simpleChoice>
          <simpleChoice identifier="guinness"> Arthur Guinness
            <feedbackInline csFeedbackId="fbGuinness" outcomeIdentifier="responses.irishPresident.value" identifier="guinness" showHide="show">Incorrect, Arthur Guinness founded Guinness Brewery</feedbackInline>
          </simpleChoice>
          <simpleChoice identifier="adams"> Gerry Adams
            <feedbackInline csFeedbackId="fbAdams" outcomeIdentifier="responses.irishPresident.value" identifier="adams" showHide="show">Incorrect, Gerry Adams is the President of the Sinn Fein political party</feedbackInline>
          </simpleChoice>
        </choiceInteraction>



        <choiceInteraction responseIdentifier="rainbowColors" shuffle="false" maxChoices="0">
          <prompt>Which colors are in a rainbow?</prompt>
          <simpleChoice identifier="blue"> Blue
            <feedbackInline showHide="show">Blue is a color in the rainbow</feedbackInline>
          </simpleChoice>
          <simpleChoice identifier="violet"> Violet
            <feedbackInline showHide="show">Violet is a color in the rainbow</feedbackInline>
          </simpleChoice>
          <simpleChoice identifier="white"> White
            <feedbackInline showHide="show">White is not a color in the rainbow.</feedbackInline>
          </simpleChoice>
          <simpleChoice identifier="red"> Red
            <feedbackInline showHide="show">Red is a color in the rainbow</feedbackInline>
          </simpleChoice>
        </choiceInteraction>



        <prompt>Identify the missing word in this famous quote from Shakespeare's Richard III.</prompt>
        <blockquote>
          <p>Now is the winter of our discontent<br/> Made glorious summer by this sun of
            <textEntryInteraction responseIdentifier="winterDiscontent" expectedLength="15"/>;<br/>
            And all the clouds that lour'd upon our house<br/> In the deep bosom of the ocean
            buried.</p>
        </blockquote>
        <p/>
        <feedbackInline
        outcomeIdentifier="responses.winterDiscontent.value"
        identifier="York"
        showHide="show">
          York is correct
        </feedbackInline>
        <feedbackInline
        outcomeIdentifier="responses.winterDiscontent.value"
        identifier="york"
        showHide="show">
          York is a proper noun, and should be capitalized.
        </feedbackInline>

        <p/>



        <p/>
        <hr/>
        <p/>


        <orderInteraction responseIdentifier="wivesOfHenry" shuffle="true">
          <prompt>Arrange the wives of Henry VIII in chronological order:</prompt>
          <simpleChoice identifier="parr">Catherine Parr</simpleChoice>
          <simpleChoice identifier="boleyn">Anne Boleyn</simpleChoice>
          <simpleChoice identifier="cleves" fixed="true">Anne of Cleves</simpleChoice>
          <simpleChoice identifier="aragon">Catherine of Aragon</simpleChoice>
          <simpleChoice identifier="seymour">Jane Seymour</simpleChoice>
          <simpleChoice identifier="howard">Catherine Howard</simpleChoice>
        </orderInteraction>


        <p/>
        <hr/>
        <p/>

        <orderInteraction responseIdentifier="cutePugs" shuffle="true">
          <prompt>Arrange these pugs in order of cuteness:</prompt>
          <simpleChoice identifier="pug1"><img src="http://stuffpoint.com/dogs/image/59240-dogs-4plus-pugs.jpg" width="150" height="150"/> </simpleChoice>
          <simpleChoice identifier="pug2"><img src="http://images.fanpop.com/images/image_uploads/pug-pugs-239511_407_436.jpg" width="150" height="150"/></simpleChoice>
          <simpleChoice identifier="pug3" fixed="true"><img src="http://www.weruletheinternet.com/wp-content/uploads/images/2011/may/cute_pugs/cute_pugs_7.jpg" width="150" height="150"/></simpleChoice>
        </orderInteraction>


        <p/>
        <hr/>
        <p/>



        <prompt>Write Sam a postcard. Write 25-35 words.</prompt>
        <extendedTextInteraction responseIdentifier="longAnswer" expectedLength="200">

        </extendedTextInteraction>

        <p/>
        <hr/>
        <p/>

      </itemBody>

      <modalFeedback
      outcomeIdentifier="responses.winterDiscontent.value"
      identifier="York"
      showHide="show">
        This is modal feedback, shown when 'York' was entered in the inline text question
      </modalFeedback>


    </assessmentItem>

    val item = new QtiItem(xml)


    /**
     * Tests for verifying json output used by TestPlayer
     *
     *  return needs to include :
     *
     * {
     *     sessionData: {
     *         feedbackContents: {
     *             [csFeedbackId]: "[contents of feedback element]",
     *             [csFeedbackId]: "[contents of feedback element]"
     *         }
     *         correctResponse: {
     *             irishPresident: "higgins",
     *             rainbowColors: ['blue','violet', 'red']
     *         }
     *     }
     *
     */


    "return a json object with all feedback accessible by csFeedbackId" in {
      val result = item.getAllFeedbackJson
      println(result)
      success
    }

    "return a json object with all correctResponses accessible by responseIdentifier" in {
      pending
    }

    // TODO: This is probably wrong.. each incorrect answer probably wants to show its own feedback.
    "should return incorrect feedback for incorrect choice" in {
      pending
      //if (item.feedback("RESPONSE", "ChoiceA") matches incorrectFeedbackIds) success else failure
    }
  }

}
