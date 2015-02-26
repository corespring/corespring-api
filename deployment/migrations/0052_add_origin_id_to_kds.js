function up() {

  db.content.find({'taskInfo.extended.kds.sourceId' : {$exists: true}}).toArray().forEach(function(item) {
    item.originId = item.taskInfo.extended.kds.sourceId;
    db.content.save(item);
  });

}

function down() {

  db.content.find({'taskInfo.extended.kds.sourceId' : {$exists: true}}).toArray().forEach(function(item) {
    delete item.originId;
    db.content.save(item);
  });

}