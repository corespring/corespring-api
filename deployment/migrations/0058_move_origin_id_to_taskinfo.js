function up() {
  // move originId to taskInfo.originId
  db.content.find({"originId": {"$exists": true}}).forEach(function(item) {
    var originId = item.originId;
    item.taskInfo.originId = originId;
    delete item.originId;
    db.content.save(item);
  });

  // if there's no originId defined, see if there's one in MP extended metadata, and copy it over if it's there
  db.content.find({"taskInfo.extended.measuredprogress.sourceId": {"$exists": true}, "taskInfo.originId": {"$exists": false}}).forEach(function(item) {
    var originId = item.taskInfo.extended.measuredprogress.sourceId;
    item.taskInfo.originId = originId;
    db.content.save(item);
  });

  // if there's no originId defined, see if there's one in KDS extended metadata, and copy it over if it's there
  db.content.find({"taskInfo.extended.kds.sourceId": {"$exists": true}, "taskInfo.originId": {"$exists": false}}).forEach(function(item) {
    var originId = item.taskInfo.extended.kds.sourceId;
    item.taskInfo.originId = originId;
    db.content.save(item);
  });
}