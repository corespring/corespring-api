function up() {
  db.content.find({"playerDefinition": {$exists: true}}).forEach(function (content) {
    var changed = false;
    for (var key in content.playerDefinition.components) {
      var comp = content.playerDefinition.components[key];
      var widths = {
        "corespring-dnd-categorize": 600,
        "corespring-drag-and-drop-categorize": 300,
        "corespring-drag-and-drop-inline": 100,
        "corespring-extended-text-entry": 100,
        "corespring-function-entry": 100,
        "corespring-graphic-gap-match": 100,
        "corespring-inline-choice": 100,
        "corespring-line": 500,
        "corespring-point-intercept": 500,
        "corespring-multiple-choice": 100,
        "corespring-match": 300,
        "corespring-number-line": 100,
        "corespring-ordering": 570
      };

      if (widths[comp.componentType]) {
        comp.minimumWidth = widths[comp.componentType];
        changed = true;
      }
    }
    if (changed) {
      db.content.save(content);
    }
  });
}

function down() {
}