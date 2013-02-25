function addIfThere(propertyName, source, target) {
  if (source[propertyName]) {
    target[propertyName] = source[propertyName];
  }
}

function hasProps(o) {
  var count = 0;
  for (var i in o) {
    count++;
  }
  return count > 0;
}

function adjustDocs(item, save) {


  function createOtherAlignments(item) {

    var otherAlignments = {};
    addIfThere("bloomsTaxonomy", item, otherAlignments);
    addIfThere("keySkills", item, otherAlignments);
    addIfThere("demonstratedKnowledge", item, otherAlignments);
    addIfThere("relatedCurriculum", item, otherAlignments);

    if (hasProps(otherAlignments)) {
      item.otherAlignments = otherAlignments;
    }

    delete item.bloomsTaxonomy;
    delete item.keySkills;
    delete item.demonstratedKnowledge;
    delete item.relatedCurriculum;
  }

  function createTaskInfo(item) {

    var taskInfo = {};
    addIfThere("subjects", item, taskInfo);
    addIfThere("gradeLevel", item, taskInfo);
    addIfThere("title", item, taskInfo);
    addIfThere("itemType", item, taskInfo);

    if (hasProps(taskInfo)) {
      item.taskInfo = taskInfo;
    }

    delete item.subjects;
    delete item.gradeLevel;
    delete item.title;
    delete item.itemType;
  }

  createOtherAlignments(item);
  createTaskInfo(item);


  save(item);
}


function revertDoc(item, saveFn) {

  if (item.taskInfo) {
    addIfThere("subjects", item.taskInfo, item);
    addIfThere("gradeLevel", item.taskInfo, item);
    addIfThere("title", item.taskInfo, item);
    addIfThere("itemType", item.taskInfo, item);
    delete item.taskInfo;
  }

  if (item.otherAlignments) {
    addIfThere("bloomsTaxonomy", item.otherAlignments, item);
    addIfThere("keySkills", item.otherAlignments, item);
    addIfThere("demonstratedKnowledge", item.otherAlignments, item);
    addIfThere("relatedCurriculum", item.otherAlignments, item);
    delete item.otherAlignments;
  }
  saveFn(item);
}

var query = {
  $or: [
    { "taskInfo": {$exists: true}},
    { "otherAlignments": {$exists: true}}
  ]
};

function up() {
  db.content.find().forEach(function (item) {
    adjustDocs(item, function (updatedObject) {
      //printjson(item);
      db.content.save(updatedObject);
    });
  });
}

function down() {
  db.content.find(query).forEach(
    function (item) {
      revertDoc(item, function (update) {
        db.content.save(update);
      })
    });
}
