var id = ObjectId("5058bd13201cd1b242d13b2a");
var template = {
  "_id" : id,
  "label" : "Inline Choice Interaction",
  "code" : "inlinechoice",
  "xmlData" : "<?xml version='1.0' encoding='UTF-8'?>\n<assessmentItem xmlns='http://www.imsglobal.org/xsd/imsqti_v2p1'\n                xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\n                xsi:schemaLocation='http://www.imsglobal.org/xsd/imsqti_v2p1 http://www.imsglobal.org/xsd/imsqti_v2p1.xsd'\n                title='' adaptive='false' timeDependent='false'>\n    <correctResponseFeedback>Looking good buddy</correctResponseFeedback>\n    <incorrectResponseFeedback>You should rethink this</incorrectResponseFeedback>\n    <!-- single choice -->\n    <responseDeclaration identifier='manOnMoon' cardinality='single' baseType='identifier'>\n        <correctResponse>\n            <value>armstrong</value>\n        </correctResponse>\n    </responseDeclaration>\n\n    <itemBody>\n        The first man on the moon was <inlineChoiceInteraction\n            responseIdentifier='manOnMoon'\n            required='false'>\n        <inlineChoice identifier='armstrong'>Neil Armstrong<feedbackInline identifier='armstrong' defaultFeedback='true'/></inlineChoice>\n        <inlineChoice identifier='aldrin'>Buzz Aldrin<feedbackInline identifier='aldrin' defaultFeedback='true'/></inlineChoice>\n    </inlineChoiceInteraction>, he landed there in 1969.\n    </itemBody>\n\n</assessmentItem>\n\n"
};

function templateExists() {
  return db.templates.find({_id:id}).count() > 0;
}

function up() {
  if (templateExists()) {
    print("Inlince Choice Template Already Exists");
  } else {
    db.templates.insert(template);
  }
}

function down() {
  if (!templateExists()) {
    print("Inlince Choice Template Doesn't Exist");
  } else {
    db.templates.remove({_id: id});
  }
}

down();
