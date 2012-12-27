var selectWordTemplate = {
    "_id" : ObjectId("5058bd133014d1b0b0a53b2d"),
    "label" : "Select Word Interaction",
    "code" : "selectword",
    "xmlData" : "<?xml version='1.0' encoding='UTF-8'?>\n<assessmentItem xmlns='http://www.imsglobal.org/xsd/imsqti_v2p1'\n                xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\n                xsi:schemaLocation='http://www.imsglobal.org/xsd/imsqti_v2p1 http://www.imsglobal.org/xsd/imsqti_v2p1.xsd'\n                title='' adaptive='false' timeDependent='false'>\n\n    <itemBody>\n        Word Example: <br/>\n        <selectTextInteraction responseIdentifier='selectText' selectionType='word' minSelections='2' maxSelections='2'>\n             'It turns out my <correct>mother</correct> loved the name Ruth. That's how I got my <correct>name</correct> and how my father got these: he let Ty Cobb name me after Babe Ruth.'\n                 I tried to swallow but couldn't! I hoped that she wasn't going to say what I thought she was going to say.\n                 Then she said it?<br/>\n                 'In this shoebox are the ten baseballs Ty Cobb gave my father. They are signed by some of the most famous ballplayers in history, including one that has one single signature on it: Babe Ruth's.'\n        </selectTextInteraction>\n\n        <feedbackBlock outcomeIdentifier='responses.selectText.outcome.responsesBelowMin' identifier='id1'>\n            <div class='feedback-block-incorrect'>You selected too few words!</div>\n        </feedbackBlock>\n\n        <feedbackBlock outcomeIdentifier='responses.selectText.outcome.responsesExceedMax' identifier='id2'>\n            <div class='feedback-block-incorrect'>You selected too many words!</div>\n        </feedbackBlock>\n\n        <feedbackBlock outcomeIdentifier='responses.selectText.outcome.responsesCorrect' identifier='id3'>\n            <div class='feedback-block-correct'>Well done you have selected the right words!</div>\n        </feedbackBlock>\n\n    </itemBody>\n\n</assessmentItem>\n\n"
};

var selectSentenceTemplate = {
    "_id" : ObjectId("5059ad143014d1b0b0a53b2e"),
    "label" : "Select Sentence Interaction",
    "code" : "selectsentence",
    "xmlData" : "<?xml version='1.0' encoding='UTF-8'?>\n<assessmentItem xmlns='http://www.imsglobal.org/xsd/imsqti_v2p1'\n                xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\n                xsi:schemaLocation='http://www.imsglobal.org/xsd/imsqti_v2p1 http://www.imsglobal.org/xsd/imsqti_v2p1.xsd'\n                title='' adaptive='false' timeDependent='false'>\n\n    <itemBody>\n        Sentence Example:<br/>\n        <selectTextInteraction responseIdentifier='selectText' selectionType='sentence' checkIfCorrect='yes' minSelections='2' maxSelections='3'>\n          '<correct>It turns out my mother loved the name Ruth</correct>. <correct>That's how I got my name and how my father got these: he let Ty Cobb name me after Babe Ruth</correct>.'\n               I tried to swallow Vikram S. Pandit but couldn't. I'm not going to the forest - said I. I hoped that she wasn't going to say what I thought she was going to say.\n               <correct>Then she said it</correct>?<br/>\n               'In this shoebox are the ten baseballs Ty Cobb gave my father. They are signed by some of the most famous ballplayers in history, including one that has one single signature on it: Babe Ruth's.'\n        </selectTextInteraction>\n\n        <feedbackBlock outcomeIdentifier='responses.selectText.outcome.responsesBelowMin' identifier='idb1'>\n           <div class='feedback-block-incorrect'>You selected too few sentences!</div>\n        </feedbackBlock>\n\n        <feedbackBlock outcomeIdentifier='responses.selectText.outcome.responsesExceedMax' identifier='idb2'>\n           <div class='feedback-block-incorrect'>You selected too many sentences!</div>\n        </feedbackBlock>\n\n        <feedbackBlock outcomeIdentifier='responses.selectText.outcome.responsesCorrect' identifier='idb3'>\n           <div class='feedback-block-correct'>Well done you have selected the right sentences!</div>\n        </feedbackBlock>\n\n        <feedbackBlock outcomeIdentifier='responses.selectText.outcome.responsesIncorrect' identifier='idb4'>\n           <div class='feedback-block-incorrect'>Some of your selection is not right!</div>\n        </feedbackBlock>\n    </itemBody>\n\n</assessmentItem>\n\n" };

db.templates.insert(selectWordTemplate);
db.templates.insert(selectSentenceTemplate);