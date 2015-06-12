function up() {
  var num = 0;
  db.content.find({"taskInfo.subjects.related": {$exists: true}}).forEach(function(content) {
    var isArray = content.taskInfo.subjects.related instanceof Array;
    if (!isArray) {
      var array = [content.taskInfo.subjects.related];
      content.taskInfo.subjects.related = array;
      db.content.save(content);
      num++;
    }
  });
  print(num + " items have been converted");
}

function down() {
  db.content.find({"taskInfo.subjects.related": {$exists: true}}).forEach(function(content) {
    var isArray = content.taskInfo.subjects.related instanceof Array;
    if (isArray) {
      var relatedSubject = content.taskInfo.subjects.related.length ? content.taskInfo.subjects.related[0] : undefined;
      content.taskInfo.subjects.related = relatedSubject;
      db.content.save(content);
    }

  });
}
