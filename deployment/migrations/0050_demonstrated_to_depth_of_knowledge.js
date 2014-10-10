var knowledgeMap = {
  'Factual' : 'Recall & Reproduction',
  'Procedural' : 'Skills & Concepts',
  'Conceptual' : 'Strategic Thinking & Reasoning',
  'Metacognitive' : 'Extended Thinking',
  'Meta-Cognitive' : 'Extended Thinking',
  'None' : 'None'
};

function up() {
  db.content.find({"otherAlignments.demonstratedKnowledge": {$exists: true}}).forEach(function(content) {
    var value = content.otherAlignments.demonstratedKnowledge;
    delete content.otherAlignments.demonstratedKnowledge;
    if (knowledgeMap[value] === undefined) {
      print("WARNING: Could not find new value for " + value);
    } else {
      content.otherAlignments.depthOfKnowledge = knowledgeMap[value];
      db.content.save(content);
    }
  });
}

function down() {

  var reverseMap = {
    'Recall & Reproduction' : 'Factual',
    'Skills & Concepts' : 'Procedural',
    'Strategic Thinking & Reasoning' : 'Conceptual',
    'Extended Thinking' : 'Metacognitive',
    'None' : 'None'
  }

  db.content.find({"otherAlignments.depthOfKnowledge": {$exists: true}}).forEach(function(content) {
    var value = content.otherAlignments.depthOfKnowledge;
    delete content.otherAlignments.depthOfKnowledge;
    if (reverseMap[value] === undefined) {
      print("WARNING: Could not find new value for " + value);
    } else {
      content.otherAlignments.demonstratedKnowledge = value;
      db.content.save(content);
    }
  });

}
