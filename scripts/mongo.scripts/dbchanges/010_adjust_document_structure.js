function adjustDocs(item, save){

  function addIfThere(propertyName, source, target){
    if(source[propertyName]){
      target[propertyName] = source[propertyName];
    }
  }

  function hasProps(o){
    var count = 0;
    for( var i in o){ count++; }
    return count > 0;
  }

  function createOtherAlignments(item){

    var otherAlignments = {};
    addIfThere("bloomsTaxonomy", item, otherAlignments );
    addIfThere("keySkills", item, otherAlignments );
    addIfThere("demonstratedKnowledge", item, otherAlignments );
    addIfThere("relatedCurriculum", item, otherAlignments );

    if(hasProps(otherAlignments)){
      item.otherAlignments = otherAlignments;
    }

    delete item.bloomsTaxonomy;
    delete item.keySkills;
    delete item.demonstratedKnowledge;
    delete item.relatedCurriculum;
  }

  function createTaskInfo(item){

    var taskInfo = {};
    addIfThere("subjects", item, taskInfo);
    addIfThere("gradeLevel", item, taskInfo);
    addIfThere("title", item, taskInfo);
    addIfThere("itemType", item, taskInfo);

    if(hasProps(taskInfo)){
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

db.content.find().forEach(function(item){ adjustDocs(item, function(updatedObject){
    //printjson(item);
    db.content.save(updatedObject);
  });
});