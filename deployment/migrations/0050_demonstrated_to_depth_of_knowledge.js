function up() {
  db.content.find({"otherAlignments.demonstratedKnowledge": {$exists: true}}).forEach(function(content) {
    var value = content.otherAlignments.demonstratedKnowledge;
    delete content.otherAlignments.demonstratedKnowledge;
    content.otherAlignments.depthOfKnowledge = value;
    db.content.save(content);
  });
}

function down() {
  db.content.find({"otherAlignments.depthOfKnowledge": {$exists: true}}).forEach(function(content) {
    var value = content.otherAlignments.depthOfKnowledge;
    delete content.otherAlignments.depthOfKnowledge;
    content.otherAlignments.demonstratedKnowledge = value;
    db.content.save(content);
  });
}