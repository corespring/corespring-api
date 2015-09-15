function migrateDepthOfKnowledge() {
  var depthOfKnowledge = {
    "1": "Recall & Reproduction",
    "2": "Skills & Concepts",
    "3": "Strategic Thinking & Reasoning",
    "4": "Extended Thinking",
    "None": "None"
  };

  for (var key in depthOfKnowledge) {
    var items = db.content.find({"otherAlignments.depthOfKnowledge": depthOfKnowledge[key]}).toArray();
    for (var i in items) {
      var item = items[i];
      item.otherAlignments.depthOfKnowledge = key;
      db.content.save(item);
    }
  }

  var fv = db.fieldValues.find({"depthOfKnowledge" : {$exists: 1}}).toArray();
  for (var i in fv) {
    for (var j in fv[i].depthOfKnowledge) {
      for (var key in depthOfKnowledge) {
        if (fv[i].depthOfKnowledge[j].key == depthOfKnowledge[key]) {
          fv[i].depthOfKnowledge[j].key = key;
        }
      }
      db.fieldValues.save(fv[i]);
    }
  }
}

function up() {
  migrateDepthOfKnowledge();
}

function down() {
  print("Irreversible migration.");
}
