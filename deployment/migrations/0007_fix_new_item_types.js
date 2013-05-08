var itemTypes = [
  {
    "key":"Fixed Choice",
    "value":["Multiple Choice","Multi-Multi Choice","Visual Multi Choice","Inline Choice","Ordering"]
  },
  {
    "key":"Constructed Response",
    "value":["Constructed Response - Short Answer","Constructed Response - Open Ended"]
  },
  {
    "key":"Evidence",
    "value":["Select Evidence in Text","Document Based Question","Passage With Questions"]
  },
  {
    "key":"Composite",
    "value":["Composite - Multiple MC","Composite - MC and SA","Composite - MC, SA, OE","Project","Performance","Activity"]
  }
];

var oldItemTypes = [
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

var mapping = {
  "Passage With Question": "Passage With Questions",
  "Ordering (Simple Placement)": "Ordering"
};

function up() {
  db.fieldValues.find().forEach(function(it) {
    it.itemTypes = itemTypes;
    db.fieldValues.save(it);
  });
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
  db.fieldValues.find().forEach(function(it) {
    it.itemTypes = oldItemTypes;
    db.fieldValues.save(it);
  });
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