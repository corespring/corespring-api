//a script that takes everything in the kds collection db.contentcolls.find({"name": "KDS"})
//and updates the title and description fields *ONLY IF THEY'RE BLANK/null/undefined* with the
//value of 'taskInfo.extended.kds.sourceId'

//the script does not care about the actual db
//the db, the credentials and the script are set in the parameters to mongo like so
//$ mongo ds063347-a1.mongolab.com:63347/corespring-staging -u corespring -p xxxxxxxx kds-set-default-titles-if-title-empty.js

function getSourceId(item){
    try {
        return item.taskInfo.extended.kds.sourceId;
    } catch(e){
        //ignore
    }
    return null;
}

function getDefaultTitle(item){
    var sourceId = getSourceId(item)
    return sourceId ? sourceId : "default title";
}

function getDefaultDescription(item){
    var sourceId = getSourceId(item);
    if(!sourceId){
        sourceId = getSourceIdFromTitle(item);
    }
    return sourceId ? sourceId : "default description";
}

function getSourceIdFromTitle(item){
    try {
        var re = /\w*(\d+)/;
        var matches = re.exec(item.taskInfo.title);
        sourceId = matches[0];
        return sourceId;
    } catch(e) {
        //ignore
    }
    return null;
}

function getKDSCollection(){
 var kdsCol = db.contentcolls.findOne({name: "KDS"});
 return kdsCol;
}

function updateKDSItems(kdsCol){
    return db.content.find({"collectionId":kdsCol._id.str}).map(function(item){
        var updates = {};
        var needsUpdate = false;
        var result = "";

        try {
            if(!item.taskInfo.title){
                updates["taskInfo.title"] = getDefaultTitle(item);
                needsUpdate = true;
            }
            if(!item.taskInfo.description){
                updates["taskInfo.description"] = getDefaultDescription(item);
                needsUpdate = true;
            }
            if(!getSourceId(item)) {
                var sourceId = getSourceIdFromTitle(item);
                if(sourceId){
                    updates["taskInfo.extended.kds.sourceId"] = sourceId;
                    needsUpdate = true;
                }
            }

            if(needsUpdate){
                db.content.update({_id: item._id}, {$set: updates});
                result = updates;
            }
        } catch(e) {
            result = "Error updating " + e;
        }
        return {itemId: item._id, result: result};
    });
}

updateKDSItems(getKDSCollection());
