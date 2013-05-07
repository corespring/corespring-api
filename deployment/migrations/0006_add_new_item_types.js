var itemTypes = [
  {
    "key":"Fixed Choice",
    "value":["Multiple Choice","Multi-Multi Choice","Visual Multi Choice","Inline Choice","Ordering (Simple Placement)"]
  },
  {
    "key":"Constructed Response",
    "value":["Constructed Response - Short Answer","Constructed Response - Open Ended"]
  },
  {
    "key":"Evidence",
    "value":["Select Evidence in Text","Document Based Question","Passage With Question"]
  },
  {
    "key":"Composite",
    "value":["Composite - Multiple MC","Composite - MC and SA","Composite - MC, SA, OE","Project","Performance","Activity"]
  }
];

var oldItemTypes = [
  {
    "key":"MC",
    "value":"Multiple Choice"
  },
  {
    "key":"TQ",
    "value":"Text with Questions"
  },
  {
    "key":"PT",
    "value":"Performance Task"
  },
  {
    "key":"ACT",
    "value":"Activity"
  },
  {
    "key":"CR-SA",
    "value":"Constructed Response - Short Answer"
  },
  {
    "key":"CR-OE",
    "value":"Constructed Response - Open Ended"
  },
  {
    "key":"FT",
    "value":"Focus Task"
  },
  {
    "key":"OR",
    "value":"Ordering"
  }
];

var mapping = {
  "Text with Questions": "Passage With Question",
  "Performance Task":"Performance",
  "Focus Task":"Visual Multi Choice",
  "Ordering":"Ordering (Simple Placement)"
};

function up() {
  var it = db.fieldValues.find()[0];
  it.itemTypes = itemTypes;
  db.fieldValues.save(it);
  db.content.find().forEach(function(item) {
    var changed = false;
    for (k in mapping) {
      if (item.taskInfo && item.taskInfo.itemType == k) {
        item.taskInfo.itemType = mapping[k];
        changed = true;
      }
    }
    if (changed) db.content.save(item);
  });
}

function down() {
  var it = db.fieldValues.find()[0];
  it.itemTypes = oldItemTypes;
  db.fieldValues.save(it);
  db.content.find().forEach(function(item) {
    var changed = false;
    for (k in mapping) {
      if (item.taskInfo && item.taskInfo.itemType == mapping[k]) {
        item.taskInfo.itemType = k;
        changed = true;
      }
    }
    if (changed) db.content.save(item);
  });
}
