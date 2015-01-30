function getSourceId(item){
    try {
        return item.taskInfo.extended.kds.sourceId;
    } catch(e){
        //ignore
    }
    return null;
}

function getScoringType(item){
    try {
        return item.taskInfo.extended.kds.scoringType;
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

function titleIsKDSSourceId(item){
    try {
        var title = item.taskInfo.title;
        var re = /\w*(\d+)/;
        var matches = re.exec(title);
        return matches != null;
    } catch(e){
        //ignore
    }
    return false;
}

function updateKDSItems(kdsCol){
    return db.content.find({"collectionId":kdsCol._id.str}).map(function(item){
        var updates = {};
        var needsUpdate = false;
        var result = "";

        try {
            if(titleIsKDSSourceId(item)){
                var scoringType = getScoringType(item);
                updates["taskInfo.title"] = item.taskInfo.title + " - " + scoringType;
                needsUpdate = true;
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

updateKDSItems(getKDSCollection())
