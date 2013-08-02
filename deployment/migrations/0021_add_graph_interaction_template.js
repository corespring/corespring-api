var templates = [
  {
    "label": "Line Interaction",
    "code": "line",
    "group": "Graph",
    "position": 4,
    "xmlData": "<?xml version='1.0' encoding='UTF-8'?>\n\n<assessmentItem>\n    <correctResponseFeedback>Looking good buddy</correctResponseFeedback>\n    <incorrectResponseFeedback>You should rethink this</incorrectResponseFeedback>\n    <responseDeclaration identifier=\"graphTest\" baseType=\"line\" cardinality=\"single\">\n        <correctResponse>\n            <value>y=2x+7</value>\n        </correctResponse>\n    </responseDeclaration>\n    <responseDeclaration identifier=\"equationEntryTest\" cardinality=\"single\" baseType=\"line\">\n        <correctResponse>\n            <value>y=2x+7</value>\n        </correctResponse>\n    </responseDeclaration>\n    <itemBody>\n\n        Line P has a slope of 2 and intersects point (-3,1). Draw line P below and give the equation for line P\n        <lineInteraction jsxgraphcore=\"\"\n                         responseIdentifier=\"graphTest\"\n                         graph-width=\"300px\"\n                         graph-height=\"300px\"\n                         domain=\"10\"\n                         range=\"10\"\n                         scale=\"1\"\n                         domain-label=\"x\"\n                         range-label=\"y\"\n                         tick-label-frequency=\"5\"\n                         sigfigs=\"-1\"\n                />\n\n        <feedbackBlock outcomeIdentifier=\"responses.graphTest.outcome.incorrect\">\n            <div class=\"feedback-block-incorrect\">Wrong!</div>\n        </feedbackBlock>\n\n        <feedbackBlock outcomeIdentifier=\"responses.graphTest.outcome.correct\">\n            <div class=\"feedback-block-correct\">You're right!</div>\n        </feedbackBlock>\n\n        <p>The equation for the line is: </p>\n        <textEntryInteraction responseIdentifier=\"equationEntryTest\" expectedLength=\"20\" />\n        <feedbackBlock outcomeIdentifier=\"responses.equationEntryTest.outcome.incorrectEquation\">\n            <div class=\"feedback-block-incorrect\">Wrong!</div>\n        </feedbackBlock>\n\n        <feedbackBlock outcomeIdentifier=\"responses.equationEntryTest.value\" identifier=\"y=2x+7\">\n            <div class=\"feedback-block-correct\">You're right!</div>\n        </feedbackBlock>\n\n        <feedbackBlock outcomeIdentifier=\"responses.equationEntryTest.outcome.lineEquationMatch\">\n            <div class=\"feedback-block-correct\">You're right, but equation is not in form of y=mx+b</div>\n        </feedbackBlock>\n\n\n    </itemBody>\n</assessmentItem>\n"
  },
  {
    "label": "Point Interaction",
    "code": "point",
    "group": "Graph",
    "position": 5,
    "xmlData": "<?xml version='1.0' encoding='UTF-8'?>\n\n<assessmentItem>\n    <correctResponseFeedback>Looking good buddy</correctResponseFeedback>\n    <incorrectResponseFeedback>You should rethink this</incorrectResponseFeedback>\n    <responseDeclaration identifier=\"interceptTest\" baseType=\"string\" cardinality=\"ordered\">\n        <correctResponse>\n            <value>0,6</value>\n            <value>-3,0</value>\n        </correctResponse>\n    </responseDeclaration>\n    <itemBody>\n\n        Give the y-intercept and x-intercept of y=2x+6\n        <pointInteraction jsxgraphcore=\"\"\n                         responseIdentifier=\"interceptTest\"\n                         graph-width=\"300px\"\n                         graph-height=\"300px\"\n                         point-labels=\"y-intercept,x-intercept\"\n                         max-points=\"2\"\n                />\n\n        <feedbackBlock outcomeIdentifier=\"responses.interceptTest.outcome.incorrect\">\n            <div class=\"feedback-block-incorrect\">Wrong!</div>\n        </feedbackBlock>\n\n        <feedbackBlock outcomeIdentifier=\"responses.interceptTest.outcome.correct\">\n            <div class=\"feedback-block-correct\">You're right!</div>\n        </feedbackBlock>\n\n    </itemBody>\n</assessmentItem>\n"
  }
];



function up() {
  for (var i=0; i<templates.length; i++)
    db.templates.insert(templates[i]);
}

function down() {
  // Non-reversible
}
