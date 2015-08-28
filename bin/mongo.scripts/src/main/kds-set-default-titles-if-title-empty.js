//a script that takes everything in the kds collection db.contentcolls.find({"name": "KDS"})
//and updates the title and description fields

//see comments at the bottom if you want to run this

/* global db */

function KDSTitleSetter() {

  function hasTaskInfo(item) {
    return item.taskInfo !== undefined;
  }

  function hasKdsData(item) {
    return hasTaskInfo(item) && item.taskInfo.extended && item.taskInfo.extended.kds;
  }

  function getSourceId(item) {
    return hasKdsData(item) ? item.taskInfo.extended.kds.sourceId : undefined;
  }

  function getScoringType(item) {
    return hasKdsData(item) ? item.taskInfo.extended.kds.scoringType : undefined;
  }

  function getKdsTitle(item) {
    var sourceId = getSourceId(item);
    var scoringType = getScoringType(item);
    return (sourceId !== undefined && scoringType !== undefined) ?
      sourceId + " - " + scoringType : undefined;
  }

  function log(message) {
    print(message);
  }

  this.processCollection = function(name) {
    log("processCollection:" + name);
    var result = [];
    var kdsCol = db.contentcolls.findOne({name: name});
    if (kdsCol) {
      log("processCollection: collection found:" + kdsCol._id.str);
      var items = db.content.find({"collectionId": kdsCol._id.str});

      log("processCollection: #items found: " + items.count());

      var updates = 0;
      items.forEach(function(item) {
        if (item._id && item._id._id) {
          print(item._id._id);
        }
        var title = getKdsTitle(item);
        if (title !== undefined) {
          updates++;
          db.content.update({"_id": item._id}, {"$set" : {"taskInfo.title" : title, "taskInfo.description" : title}});
          var updated = db.content.findOne({"_id": item._id});
        } else {
          print("WARNING: could not create title for" + item._id._id + ":" + item._id.version);
        }
      });

      log("processCollection: items processed, #updates: " + updates);
    }
  };
}

var processor = new KDSTitleSetter();
processor.processCollection("KDS");
