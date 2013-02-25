var fileChange = {
  "models.VirtualFile": "models.item.resource.VirtualFile",
  "models.StoredFile": "models.item.resource.StoredFile"
};

var sessionChange = {
  "models.StringItemResponse": "models.itemSession.StringItemResponse",
  "models.ArrayItemResponse": "models.itemSession.ArrayItemResponse"
};

var fileChangeDown = {};
for (var x in fileChange) {
  var value = fileChange[x];
  fileChangeDown[value] = x;
}

var sessionChangeDown = {};
for (var z in sessionChange) {
  var valueTwo = sessionChange[x];
  sessionChangeDown[valueTwo] = x;
}

function process(filesArray, changeMap) {

  if (!filesArray) {
    return false;
  }
  var changed = false;
  for (var i = 0; i < filesArray.length; i++) {
    var f = filesArray[i];
    if (f._t && changeMap[f._t]) {
      f._t = changeMap[f._t];
      changed = true;
    }
  }
  return changed;
}

function up() {
  db.content.find({"data.files._t": {$exists: true}}).forEach(function (item) {
    var changed = process(item.data.files, fileChange);
    if (changed) {
      db.content.save(item);
    }
  });

  db.content.find({"supportingMaterials.files._t": {$exists: true}}).forEach(function (item) {
    var changed = process(item.supportingMaterials.files, fileChange);
    if (changed) {
      db.content.save(item);
    }
  });

  db.itemsessions.find({"responses._t": {$exists: true}}).forEach(function (session) {
    var changed = process(session.responses, sessionChange);
    if (changed) {
      db.itemsessions.save(session);
    }
  });

}

function down() {

  db.content.find({"data.files._t": {$exists: true}}).forEach(function (item) {
    var changed = process(item.data.files, fileChangeDown);
    if (changed) {
      db.content.save(item);
    }
  });

  db.content.find({"supportingMaterials.files._t": {$exists: true}}).forEach(function (item) {
    var changed = process(item.supportingMaterials.files, fileChangeDown);
    if (changed) {
      db.content.save(item);
    }
  });

  db.itemsessions.find({"responses._t": {$exists: true}}).forEach(function (session) {
    var changed = process(session.responses, sessionChangeDown);
    if (changed) {
      db.itemsessions.save(session);
    }
  });
}

