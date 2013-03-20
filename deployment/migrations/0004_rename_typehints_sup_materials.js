var fileChange = {
  "models.VirtualFile": "models.item.resource.VirtualFile",
  "models.StoredFile": "models.item.resource.StoredFile"
};


var fileChangeDown = {};
for (var x in fileChange) {
  var value = fileChange[x];
  fileChangeDown[value] = x;
}


function process(filesArray, changeMap) {

  if (!filesArray) {
    return false;
  }
  var changed = false;
  for (var i = 0; i < filesArray.length; i++) {
    var f = filesArray[i];
    print(f._t);
    if (f._t && changeMap[f._t]) {
      f._t = changeMap[f._t];
      changed = true;
    }
  }
  return changed;
}


function up() {
  db.content.find({"supportingMaterials.files._t": {$exists: true}}).forEach(function (item) {

    for(var i = 0 ; i < item.supportingMaterials.length; i++){
      var sm = item.supportingMaterials[i];
      //printjson(sm.files);
      var changed = process(sm.files, fileChange);
      if (changed) {
        printjson(sm.files);
        db.content.save(item);
      }
    }
  });
}

function down() {
  db.content.find({"supportingMaterials.files._t": {$exists: true}}).forEach(function (item) {
    for(var i = 0; i < item.supportingMaterials.length; i++){
      var sm = item.supportingMaterials[i];
      var changed = process(sm.files, fileChangeDown);
      if (changed) {
        db.content.save(item);
      }
    }
  });
}

