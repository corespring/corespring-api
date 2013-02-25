function hasKey(it, key) {
  for (var i = 0; i < it.length; i++) {
    if (it[i].key == key) return true;
  }
  return false;
}

function up() {

  db["fieldValues"].find().forEach(function (s) {
    var itemTypes = s.itemTypes;
    if (!hasKey(itemTypes, "FT"))
      itemTypes.push({key: 'FT', value: 'Focus Task'});
    else
      print("Focus Task is already present");

    if (!hasKey(itemTypes, "OR"))
      itemTypes.push({key: 'OR', value: 'Ordering'});
    else
      print("Ordering task is already present");

    db["fieldValues"].save(s);
  });
}

function down() {
  db.fieldValues.find().forEach(function (fv) {
    var filtered = [];
    for (var i = 0; i < fv.itemTypes.length; i++) {
      var t = itemType[i];
      if (t.key != "FT" && t.key != "OR") {
        filtered.push(t);
      }
    }
    fv.itemTypes = filtered;
    db.fieldValues.save(fv);

  });
}
