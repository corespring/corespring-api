//a script that takes everything in the kds collection db.contentcolls.find({"name": "KDS"})
//and updates the title and description fields *ONLY IF THEY'RE BLANK/null/undefined* with the
//value of 'taskInfo.extended.kds.sourceId'

//the script does not care about the actual db
//the db, the credentials and the script are set in the parameters to mongo like so
//$ mongo ds035160-a0.mongolab.com:35160/corespring-staging -u corespring -p xxxxxxxx kds-set-default-titles-if-title-empty.js

/* global db */

function KdsSetDefaultTitlesIfTitleEmpty(db) {

  function emptyIfAutoValue(value) {
    return (value && value.indexOf('#auto#') === -1) ? value : '';
  }

  function getSourceId(item) {
    try {
      return emptyIfAutoValue(item.taskInfo.extended.kds.sourceId);
    } catch (e) {
      //ignore
    }
    return '';
  }

  function getScoringType(item) {
    try {
      return emptyIfAutoValue(item.taskInfo.extended.kds.scoringType);
    } catch (e) {
      //ignore
    }
    return '';
  }

  function getTitle(item) {
    try {
      return emptyIfAutoValue(item.taskInfo.title);
    } catch (e) {
      //ignore
    }
    return '';
  }

  function getDescription(item) {
    try {
      return emptyIfAutoValue(item.taskInfo.description);
    } catch (e) {
      //ignore
    }
    return '';
  }

  function getSourceIdFromTitle(item) {
    try {
      var re = /^\s*(\d+)/;
      var matches = re.exec(getTitle(item));
      var sourceId = matches[0];
      return sourceId;
    } catch (e) {
      //ignore
    }
    return '';
  }

  function checkIfUpdateIsNeeded(item, auto) {
    var updates = {};
    var result = {itemId: item._id, type: "NO CHANGE", updates: updates, item: item};

    auto = auto || '';

    try {
      var sourceId = getSourceId(item);
      if (!sourceId) {
        sourceId = getSourceIdFromTitle(item);
        if (sourceId) {
          updates["taskInfo.extended.kds.sourceId"] = sourceId + auto;
          result.type = "UPDATE";
        }
      }
      var scoringType = getScoringType(item);

      if (!getTitle(item)) {
        updates["taskInfo.title"] = sourceId + " - " + scoringType + auto;
        result.type = "UPDATE";
      }

      if (!getDescription(item)) {
        updates["taskInfo.description"] = sourceId + " - " + scoringType + auto;
        result.type = "UPDATE";
      }

    } catch (e) {
      result.type = "ERROR";
      result.error = "Error updating " + e;
    }
    return result;
  }

  var _log = [];

  function log(message, type) {
    _log.push({type: type ? type : "LOG", message: message});
  }

  function processCollection(name) {
    log("processCollection:" + name);
    var result = [];
    var kdsCol = db.contentcolls.findOne({name: name});
    if (kdsCol) {
      log("processCollection: collection found:" + kdsCol._id.str);
      var items = db.content.find({"collectionId": kdsCol._id.str});
      log("processCollection: #items found:" + items.count());
      var updates = 0;
      result = items.map(function (item) {
        var maybeUpdate = checkIfUpdateIsNeeded(item, ' #auto#');
        if (maybeUpdate.type === "UPDATE") {
          updates++;
          db.content.update({_id: maybeUpdate.itemId}, {$set: maybeUpdate.updates});
        }
        return maybeUpdate;
      });
      log("processCollection: items processed, #updates:" + updates);
    } else {
      log("processCollection: collection not found" + name, "ERROR");
    }
    return result;
  }

  function main() {
    log("main");
    var resKDS = processCollection("KDS");
    var resKDSUpdate = processCollection("KDSUpdate");
    var logs = [{logs: _log}];
    return logs.concat(resKDS || []).concat(resKDSUpdate || []);
  }


  this.checkIfUpdateIsNeeded = checkIfUpdateIsNeeded;
  this.main = main;
}

//var processor = new KdsSetDefaultTitlesIfTitleEmpty(db);
//processor.main();

