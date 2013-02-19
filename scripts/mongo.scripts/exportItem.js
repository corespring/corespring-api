function fix(obj) {
  delete obj.dateModified;
  obj._id = {$oid: obj._id.toString()};
  if (obj.taskInfo == undefined) {
    obj.taskInfo = {
      subjects: obj.subjects,
      gradeLevel: obj.gradeLevel,
      title: obj.title
    };
    delete obj.subjects;
    delete obj.gradeLevel;
    delete obj.title;
  }
  if (obj.taskInfo && obj.taskInfo.subjects && obj.taskInfo.subjects.primary)
    obj.taskInfo.subjects.primary = {$oid: obj.taskInfo.subjects.primary.toString()};

  if (obj.taskInfo && obj.taskInfo.subjects && obj.taskInfo.subjects.related)
    obj.taskInfo.subjects.related = {$oid: obj.taskInfo.subjects.related.toString()};

}

db.content.find({_id:ObjectId(id)}).forEach( function(s){
  var json = s;
  fix(json);
  printjson(s);
})