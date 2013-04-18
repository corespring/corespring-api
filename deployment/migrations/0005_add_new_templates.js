var id = ObjectId("5058bd13201cd1b242d13b2a");

var templates = [
  {
    "label": "Multiple Choice",
    "code": "multiplechoice",
    "group": "Fixed Choice",
    "xmlData": '<?xml version="1.0" encoding="UTF-8"?>\n    <!-- This example adapted from the PET Handbook, copyright University of Cambridge ESOL Examinations -->\n    <assessmentItem xmlns="http://www.imsglobal.org/xsd/imsqti_v2p1"\n    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"\n    xsi:schemaLocation="http://www.imsglobal.org/xsd/imsqti_v2p1 http://www.imsglobal.org/xsd/imsqti_v2p1.xsd"\n    identifier="composite" title="Composite Item" adaptive="false" timeDependent="false">\n\n      <correctResponseFeedback>Correct!</correctResponseFeedback>\n      <incorrectResponseFeedback>Your answer.</incorrectResponseFeedback>\n\n      <responseDeclaration identifier="Q_01" cardinality="single" baseType="identifier">\n        <correctResponse>\n          <value>ChoiceA</value>\n        </correctResponse>\n      </responseDeclaration>\n\n      <outcomeDeclaration identifier="SCORE" cardinality="single" baseType="integer">\n        <defaultValue>\n          <value>0</value>\n        </defaultValue>\n      </outcomeDeclaration>\n\n      <itemBody>\n\n        <p class="intro-2">INTRO HERE</p>\n\n        <div class="imagewrapper">\n          <img src="IMAGE.png"/>\n        </div>\n\n        <choiceInteraction responseIdentifier="Q_01" shuffle="false" maxChoices="1">\n          <prompt>ITEM PROMPT?</prompt>\n          <simpleChoice identifier="ChoiceA">ChoiceA text (Correct Choice)<feedbackInline identifier="ChoiceA" defaultFeedback="true"/></simpleChoice>\n          <simpleChoice identifier="ChoiceB">ChoiceB text<feedbackInline identifier="ChoiceB" defaultFeedback="true"/></simpleChoice>\n          <simpleChoice identifier="ChoiceC">ChoiceC text<feedbackInline identifier="ChoiceC" defaultFeedback="true"/></simpleChoice>\n          <simpleChoice identifier="ChoiceD">ChoiceD text<feedbackInline identifier="ChoiceD" defaultFeedback="true"/></simpleChoice>\n        </choiceInteraction>\n      </itemBody>\n\n    </assessmentItem>'
  },
  {
    "label": "Multi-Multi Choice",
    "code": "multimultichoice",
    "group": "Fixed Choice",
    "xmlData": '<?xml version="1.0" encoding="UTF-8"?>\n    <assessmentItem xmlns="http://www.imsglobal.org/xsd/imsqti_v2p1"\nxmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"\nxsi:schemaLocation="http://www.imsglobal.org/xsd/imsqti_v2p1 http://www.imsglobal.org/xsd/imsqti_v2p1.xsd"\ntitle="" adaptive="false" timeDependent="false">\n\n  <correctResponseFeedback>Correct!</correctResponseFeedback>\n  <incorrectResponseFeedback>Your answer.</incorrectResponseFeedback>\n\n\n<responseDeclaration identifier="Q_01" cardinality="multiple" baseType="identifier">\n\n  <correctResponse>\n    <value>ChoiceA</value>\n    <value>ChoiceD</value>\n  </correctResponse>\n\n  <mapping lowerBound="0" upperBound="2" defaultValue="-2">\n    <mapEntry mapKey="ChoiceA" mappedValue="1"/>\n    <mapEntry mapKey="ChoiceD" mappedValue="1"/>\n  </mapping>\n</responseDeclaration>\n<outcomeDeclaration identifier="SCORE" cardinality="single" baseType="float"/>\n\n  <itemBody>\n\n    <p class="intro-2">INTRO HERE</p>\n\n    <div class="imagewrapper">\n      <img src="PUT YOUR IMAGE HERE.png"/>\n    </div>\n\n    <choiceInteraction responseIdentifier="Q_01" shuffle="false" maxChoices="0">\n\n      <prompt>ITEM PROMPT?</prompt>\n      <simpleChoice identifier="ChoiceA" fixed="false">ChoiceA<feedbackInline identifier="ChoiceA" defaultFeedback="true"/></simpleChoice>\n      <simpleChoice identifier="ChoiceB" fixed="false">ChoiceB<feedbackInline identifier="ChoiceB" defaultFeedback="true"/></simpleChoice>\n      <simpleChoice identifier="ChoiceC" fixed="false">ChoiceC<feedbackInline identifier="ChoiceC" defaultFeedback="true"/></simpleChoice>\n      <simpleChoice identifier="ChoiceD" fixed="false">ChoiceD<feedbackInline identifier="ChoiceD" defaultFeedback="true"/></simpleChoice>\n      <simpleChoice identifier="ChoiceE" fixed="false">ChoiceE<feedbackInline identifier="ChoiceE" defaultFeedback="true"/></simpleChoice>\n      <simpleChoice identifier="ChoiceF" fixed="false">ChoiceF<feedbackInline identifier="ChoiceF" defaultFeedback="true"/></simpleChoice>\n    </choiceInteraction>\n\n\n  </itemBody>\n  </assessmentItem>'
  },
  {
    "label": "Visual Multi Choice",
    "code": "visualmultichoice",
    "group": "Fixed Choice",
    "xmlData": '<?xml version="1.0" encoding="UTF-8"?>\n<assessmentItem xmlns="http://www.imsglobal.org/xsd/imsqti_v2p1"\n                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"\n                xsi:schemaLocation="http://www.imsglobal.org/xsd/imsqti_v2p1 http://www.imsglobal.org/xsd/imsqti_v2p1.xsd"\n                title="" adaptive="false" timeDependent="false">\n\n    <correctResponseFeedback>Correct!</correctResponseFeedback>\n    <incorrectResponseFeedback>Your answer.</incorrectResponseFeedback>\n\n    <outcomeDeclaration identifier="SCORE" cardinality="single" baseType="float"/>\n\n    <responseDeclaration identifier="Q_01" cardinality="multiple" baseType="identifier">\n<!-- You can also set cardinality to "single" if there"s only one answer. If you\'d like students to actually pick multiple answers, set up a <mapping> statement as in multi-multi choice -->	\n        <correctResponse>\n            <value>A</value>\n            <value>D</value>\n            <value>E</value>\n        </correctResponse>\n    </responseDeclaration>\n\n    <itemBody>\n        <focusTaskInteraction responseIdentifier="Q_01" checkIfCorrect="yes" itemShape="square" minSelections="2" maxSelections="3" shuffle="false">\n            <prompt>Here is an item prompt which is asking the student to select a minimum of 2 and a maximum of 3 of the elements below.</prompt> \n            <focusChoice identifier="A">Option A</focusChoice>\n            <focusChoice identifier="B">Option B</focusChoice>\n            <focusChoice identifier="C">Option C</focusChoice>\n            <focusChoice identifier="D">Option D</focusChoice> \n            <focusChoice identifier="E">Option E</focusChoice>\n            <focusChoice identifier="F">Option F</focusChoice>\n        </focusTaskInteraction>\n\n        <feedbackBlock outcomeIdentifier="responses.Q_01.outcome.responsesBelowMin" identifier="id1">\n            <div class="feedback-block-incorrect">Good try, but you didn\'t select enough items.</div>\n        </feedbackBlock>\n\n        <feedbackBlock outcomeIdentifier="responses.Q_01.outcome.responsesExceedMax" identifier="id2">\n            <div class="feedback-block-incorrect">Good try, but you selected too many items.</div>\n        </feedbackBlock>\n\n        <feedbackBlock outcomeIdentifier="responses.Q_01.outcome.responsesCorrect" identifier="id3">\n            <div class="feedback-block-correct">Your selection is correct!</div>\n        </feedbackBlock>\n\n        <feedbackBlock outcomeIdentifier="responses.Q_01.outcome.responsesIncorrect" identifier="idb4">\n           <div class="feedback-block-incorrect">The correct options were Options A and D.</div>\n        </feedbackBlock>\n\n    </itemBody>\n\n</assessmentItem>'
  },
  {
    "label": "Inline Choice",
    "code": "inlinechoice",
    "group": "Fixed Choice",
    "xmlData": '<?xml version="1.0" encoding="UTF-8"?>\n<assessmentItem xmlns="http://www.imsglobal.org/xsd/imsqti_v2p1"\n    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"\n    xsi:schemaLocation="http://www.imsglobal.org/xsd/imsqti_v2p1 http://www.imsglobal.org/xsd/imsqti_v2p1.xsd"\n    identifier="composite" title="Composite Item" adaptive="false" timeDependent="false">\n    \n     <correctResponseFeedback>Correct!</correctResponseFeedback>\n    <incorrectResponseFeedback>Your Answer.</incorrectResponseFeedback>\n\n   <responseDeclaration identifier="Q_01" cardinality="single" baseType="identifier">\n        <correctResponse>\n            <value>3</value> <!-- # of choice with CORRECT ANSWER -->\n        </correctResponse>\n    </responseDeclaration>\n    \n    \n<itemBody>\n<p class="intro-2">INTRO TEXT GOES HERE.</p>  \n<p class="prompt">PROMPT?</p> \n     <p> <inlineChoiceInteraction\n            responseIdentifier="Q_01"\n            required="false">\n            <inlineChoice identifier="1">RESPONSE1<feedbackInline identifier="1">CORRECT ANSWER</feedbackInline></inlineChoice>\n            <inlineChoice identifier="2">RESPONSE2<feedbackInline identifier="2">CORRECT ANSWER</feedbackInline></inlineChoice>\n            <inlineChoice identifier="3">CORRECT ANSWER<feedbackInline identifier="3" defaultFeedback="true"/></inlineChoice>\n        </inlineChoiceInteraction> \n</p>\n\n    \n</itemBody>\n</assessmentItem>'
  },
  {
    "label": "Ordering",
    "code": "ordering",
    "group": "Fixed Choice",
    "xmlData": '<?xml version="1.0" encoding="UTF-8"?>\n<assessmentItem xmlns="http://www.imsglobal.org/xsd/imsqti_v2p1"\n                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"\n                xsi:schemaLocation="http://www.imsglobal.org/xsd/imsqti_v2p1 http://www.imsglobal.org/xsd/imsqti_v2p1.xsd"\n                title="" adaptive="false" timeDependent="false">\n\n\n    <responseDeclaration identifier="alphabet" cardinality="ordered" baseType="identifier">\n\n<!-- add csOrderingType="placement" AND orientation="horizontal" or orientation="vertical" for an ordering placement question -->\n\n        <correctResponse>\n            <value>a</value>\n            <value>b</value>\n            <value>c</value>\n            <value>d</value>\n            <value>e</value>\n            <value>f</value>\n            <value>g</value>\n            <value>h</value>\n        </correctResponse>\n    </responseDeclaration>\n\n    <itemBody>\n        <orderInteraction responseIdentifier="alphabet" shuffle="true">\n<!-- If you want potential choices to remain in the same order, set shuffle="false". Individual choice that you want in the same place should have fixed="true" -->\n            <prompt>Arrange letters of the alphabet:</prompt>\n            <simpleChoice identifier="a" fixed="true">A</simpleChoice>\n            <simpleChoice identifier="b">B</simpleChoice>\n            <simpleChoice identifier="c">C</simpleChoice>\n            <simpleChoice identifier="d">D</simpleChoice>\n            <simpleChoice identifier="e">E</simpleChoice>\n            <simpleChoice identifier="f">F</simpleChoice>\n            <simpleChoice identifier="g">G</simpleChoice>\n            <simpleChoice identifier="h" fixed="true">H</simpleChoice>\n        </orderInteraction>\n\n\n\n    </itemBody>\n\n</assessmentItem>'
  },
  {
    "label": "Correct Response Short Text Entry",
    "code": "crshorttext",
    "group": "Text Entry",
    "xmlData": '<?xml version="1.0" encoding="UTF-8"?>\n<assessmentItem xmlns="http://www.imsglobal.org/xsd/imsqti_v2p1"\n                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"\n                xsi:schemaLocation="http://www.imsglobal.org/xsd/imsqti_v2p1 http://www.imsglobal.org/xsd/imsqti_v2p1.xsd"\n                identifier="composite" title="Composite Item" adaptive="false" timeDependent="false">\n\n    <!-- definition for question one, a short text entry response with correct answer = "CORRECT ANSWER" -->\n\n    <responseDeclaration identifier="Q_01" cardinality="single" baseType="string">\n        <correctResponse>\n            <value>correct answer</value>\n            <value>Correct answer</value>\n            <value>Correct Answer</value>\n            <value>CORRECT ANSWER</value>\n        </correctResponse>\n    </responseDeclaration>\n\n    <itemBody>\n\n        <p class="prompt">PROMPT?</p>\n\n        <p class="p-indent-20">TEXT<textEntryInteraction responseIdentifier="Q_01" expectedLength="5"/>TEXT\n        </p>\n\n\n        <!-- Every possible correct response must have a feedbackBlock -->\n\n        <feedbackBlock\n                outcomeIdentifier="responses.Q_01.value"\n                identifier="correct answer">\n            <div class="feedback-block-correct">Nice work, that\'s correct!</div>\n        </feedbackBlock>\n\n        <feedbackBlock\n                outcomeIdentifier="responses.Q_01.value"\n                identifier="Correct answer">\n            <div class="feedback-block-correct">Nice work, that\'s correct!</div>\n        </feedbackBlock>\n\n        <feedbackBlock\n                outcomeIdentifier="responses.Q_01.value"\n                identifier="Correct Answer">\n            <div class="feedback-block-correct">Nice work, that\'s correct!</div>\n        </feedbackBlock>\n\n        <feedbackBlock\n                outcomeIdentifier="responses.Q_01.value"\n                identifier="CORRECT ANSWER">\n            <div class="feedback-block-correct">Nice work, that\'s correct!</div>\n        </feedbackBlock>\n\n        <!-- this is for any response not defined as a correct response in responseDeclaration -->\n        <feedbackBlock\n                outcomeIdentifier="responses.Q_01.value"\n                incorrectResponse="true">\n            <div class="feedback-block-incorrect">Good try, but the answer is CORRECT ANSWER.</div>\n        </feedbackBlock>\n    </itemBody>\n</assessmentItem>'
  },
  {
    "label": "Open Ended Extended Text Entry",
    "code": "openendedtext",
    "group": "Text Entry",
    "xmlData": '<?xml version="1.0" encoding="UTF-8"?>\n<assessmentItem xmlns="http://www.imsglobal.org/xsd/imsqti_v2p1"\n                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"\n                xsi:schemaLocation="http://www.imsglobal.org/xsd/imsqti_v2p1 http://www.imsglobal.org/xsd/imsqti_v2p1.xsd"\n                identifier="composite" title="Composite Item" adaptive="false" timeDependent="false">\n\n    <!-- an Extended text entry response for longer form text entry -->\n    <responseDeclaration identifier="Q_01" cardinality="single" baseType="string"/>\n\n    <itemBody>\n        <p class="intro-2">INTRO HERE</p>\n\n        <div class="imagewrapper">\n            <img src="IMAGE GOES HERE.png"/>\n        </div>\n\n        <p class="prompt">PROMPT?</p>\n        <extendedTextInteraction responseIdentifier="Q_01" expectedLines="5"/>\n\n    </itemBody>\n</assessmentItem>'
  },
  {
    "label": "Select Word / Sentence",
    "code": "selectword",
    "group": "Evidence",
    "xmlData": '<?xml version="1.0" encoding="UTF-8"?>\n<assessmentItem xmlns="http://www.imsglobal.org/xsd/imsqti_v2p1"\n                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"\n                xsi:schemaLocation="http://www.imsglobal.org/xsd/imsqti_v2p1 http://www.imsglobal.org/xsd/imsqti_v2p1.xsd"\n                title="" adaptive="false" timeDependent="false">\n\n    <itemBody>\n        <p class="prompt"/>GIVE STUDENTS INSTRUCTIONS HERE.\n        <p />\n        <selectTextInteraction responseIdentifier="selectText" selectionType="word" checkIfCorrect="yes"\n                               minSelections="2" maxSelections="2">\n\n            <!-- You can also set selectionType to "sentence" and set off sentences within <correct> statements. Punctuation must go OUTSIDE <correct> statements. -->\n\n            "It turns out my <correct>mother</correct> loved the name Ruth. That\'s how I got my <correct>name</correct> and how my father got these: he let Ty Cobb name me after Babe Ruth."\n            <br/>\n            I tried to swallow but couldn\'t! I hoped that she wasn\'t going to say what I thought she was going to say.\n            <br/>\n            Then she said it.\n            <br/>\n            "In this shoebox are the ten baseballs Ty Cobb gave my father. They are signed by some of the most famous ballplayers in history, including one that has one single signature on it: Babe Ruth\'s."\n        </selectTextInteraction>\n\n        <feedbackBlock outcomeIdentifier="responses.selectText.outcome.responsesBelowMin" identifier="id1">\n            <div class="feedback-block-incorrect">Good try, but please choose two words.</div>\n        </feedbackBlock>\n\n        <feedbackBlock outcomeIdentifier="responses.selectText.outcome.responsesExceedMax" identifier="id2">\n            <div class="feedback-block-incorrect">Good try, but either you chose too many words, or did not choose the\n                right ones.\n            </div>\n        </feedbackBlock>\n\n        <feedbackBlock outcomeIdentifier="responses.selectText.outcome.responsesCorrect" identifier="id3">\n            <div class="feedback-block-correct">Well done!</div>\n        </feedbackBlock>\n\n        <feedbackBlock outcomeIdentifier="responses.selectText.outcome.responsesIncorrect" identifier="idb4">\n            <div class="feedback-block-incorrect">The two words that best answer the question are "mother" and "name."\n            </div>\n        </feedbackBlock>\n\n    </itemBody>\n</assessmentItem>'
  },
  {
    "label": "Document Based Question",
    "code": "documentbased",
    "group": "Passage",
    "xmlData": '<!-- This example adapted from the PET Handbook, copyright University of Cambridge ESOL Examinations -->\n<assessmentItem xmlns="http://www.imsglobal.org/xsd/imsqti_v2p1"\n                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"\n                xsi:schemaLocation="http://www.imsglobal.org/xsd/imsqti_v2p1 http://www.imsglobal.org/xsd/imsqti_v2p1.xsd"\n                title="" adaptive="false" timeDependent="false">\n\n    <correctResponseFeedback>Correct!</correctResponseFeedback>\n    <incorrectResponseFeedback>Your answer.</incorrectResponseFeedback>\n\n    <!-- Multiple Choice Question -->\n    <responseDeclaration identifier="Q_01" cardinality="single" baseType="identifier">\n        <correctResponse>\n            <value>ChoiceA</value>\n        </correctResponse>\n    </responseDeclaration>\n    <outcomeDeclaration identifier="SCORE" cardinality="single" baseType="integer">\n        <defaultValue>\n            <value>0</value>\n        </defaultValue>\n    </outcomeDeclaration>\n\n\n    <!-- an Extended text entry response for longer form text entry -->\n    <responseDeclaration identifier="Q_02" cardinality="single" baseType="string"/>\n\n    <itemBody>\n\n        <p class="intro-2">INTRO HERE</p>\n\n        <cs-tabs>\n            <cs-tab title="TAB TITLE">\n\n                <resource\n                        type="passage"\n                        title="some name"\n                        copyrightOwner="owner"\n                        copyrightYear="yyyy"\n                        copyrightExpires="yyyy"\n                        cost="$xx.xx"\n                        licenseType="CC BY/CC BY-NC/CC BY-ND/CC BY NC-SA"\n                        url="http://www.xxx.com"\n                        />\n\n                <h1 class="text-centered">TITLE</h1>\n                <center class="author-name">by Author\'s Name</center>\n\n\n                <div class="numbered-lines">\n                    <p class="leading-100-percent">\n\n                        <line>PASSAGE HERE</line>\n                        <line>PASSAGE HERE</line>\n                        <line>PASSAGE HERE</line>\n\n                    </p>\n                </div>\n\n                <div class="note-tip">PASSAGE CREDIT GOES HERE. GET PERMISSION FROM PUBLISHER.</div>\n\n            </cs-tab>\n            <!-- Add additional tabbed text here, within a <cs-tab> statement -->\n        </cs-tabs>\n\n        <choiceInteraction responseIdentifier="Q_01" shuffle="false" maxChoices="1">\n            <prompt>MULTIPLE-CHOICE QUESTION ABOUT PASSAGE?</prompt>\n            <simpleChoice identifier="ChoiceA">ChoiceA (Correct Choice)\n                <feedbackInline identifier="ChoiceA" defaultFeedback="true"/>\n            </simpleChoice>\n            <simpleChoice identifier="ChoiceB">ChoiceB\n                <feedbackInline identifier="ChoiceB" defaultFeedback="true"/>\n            </simpleChoice>\n            <simpleChoice identifier="ChoiceC">ChoiceC\n                <feedbackInline identifier="ChoiceC" defaultFeedback="true"/>\n            </simpleChoice>\n            <simpleChoice identifier="ChoiceD">ChoiceD\n                <feedbackInline identifier="ChoiceD" defaultFeedback="true"/>\n            </simpleChoice>\n        </choiceInteraction>\n\n\n        <p class="prompt">ITEM PROMPT FOR ESSAY QUESTION?</p>\n        <extendedTextInteraction responseIdentifier="Q_02" expectedLines="5"/>\n\n\n    </itemBody>\n</assessmentItem>'
  }

];


function up() {
  db.templates.remove();

  for (var i=0; i<templates.length; i++)
    db.templates.insert(templates[i]);
}

function down() {
  // Non-reversible
}


up();