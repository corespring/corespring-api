//a script that takes everything in the kds collection db.contentcolls.find({"name": "KDS"})
//and updates the title and description fields *ONLY IF THEY'RE BLANK/null/undefined* with the
//value of 'taskInfo.extended.kds.sourceId'

//the script does not care about the actual db
//the db, the credentials and the script are set in the parameters to mongo like so
//$ mongo ds063347-a1.mongolab.com:63347/corespring-staging -u corespring -p xxxxxxxx kds-set-default-titles-if-title-empty.js

function KdsSetDefaultTitlesIfTitleEmpty(db) {

  function getSourceId(item) {
    try {
      return item.taskInfo.extended.kds.sourceId;
    } catch (e) {
      //ignore
    }
    return '';
  }

  function getScoringType(item) {
    try {
      return item.taskInfo.extended.kds.scoringType;
    } catch (e) {
      //ignore
    }
    return '';
  }

  function getTitle(item) {
    try {
      return item.taskInfo.title;
    } catch (e) {
      //ignore
    }
    return '';
  }

  function getDescription(item) {
    try {
      return item.taskInfo.description;
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

  function titleIsSourceId(item) {
    try {
      var re = /^\s*(\d+)\s*$/;
      var matches = re.exec(getTitle(item));
      return matches != null;
    } catch (e) {
      //ignore
    }
    return false;
  }

  function findCollectionByName(name){
    return db.contentcolls.findOne({name: name});
  }


  function getKDSItems(kdsCol) {
    db.content.find({"collectionId": kdsCol._id.str});
  }

  function findUpdates(items) {
    return items.map(processItem);
  }

  function processItem(item) {
    var updates = {};
    var needsUpdate = false;
    var result = {itemId: item._id};

    try {
      var sourceId = getSourceId(item);
      if(!sourceId){
        sourceId = getSourceIdFromTitle(item);
        if(sourceId){
          updates["taskInfo.extended.kds.sourceId"] = sourceId;
          needsUpdate = true;
        }
      }
      var scoringType = getScoringType(item);

      if (!getTitle(item)) {
        updates["taskInfo.title"] = sourceId + " - " + scoringType;
        needsUpdate = true;
      }

      if (!getDescription(item)) {
        updates["taskInfo.description"] = sourceId;
        needsUpdate = true;
      }

      if (needsUpdate) {
        result.needsUpdate = true;
        result.updates = updates;
      }
    } catch (e) {
      result.error = "Error updating " + e;
    }
    return result;
  }

  function writeUpdates(updates) {
    return updates.map(function (update) {
      try {
        db.content.update({_id: update.itemId}, {$set: update.updates});
      } catch(e){
        update.error = "Error " + e;
      }
      return update;
    });
  }

  function processCollection(name){
    var kdsCol = getCollectionByName(name);
    var items = getKDSItems(kdsCol);
    var updates = findUpdates(items);
    return writeUpdates(updates);
  }

  function main(){
    var resKDS = processCollection("KDS");
    var resKDSUpdate = processCollection("KDSUpdates");
    return resKDS.concat(resKDSUpdate);
  }

  this.main = main;
  this.findUpdates = findUpdates;
  this.processItem = processItem;
}