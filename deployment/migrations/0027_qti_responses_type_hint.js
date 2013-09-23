//repackaging has taken place for corespring-qti responses - update type hints
//models.itemSession.StringItemResponse => org.corespring.qti.models.responses.StringResponse
//models.itemSession.ArrayItemResponse => org.corespring.qti.models.responses.ArrayResponse

function up() {
    db.itemsessions.update(
        {"responses._t":"models.itemSession.StringItemResponse"},
        {"$set":{"responses.$._t":"org.corespring.qti.models.responses.StringResponse"}},
        {multi:true}
    )
    while(db.itemsessions.find({"responses._t":"models.itemSession.StringItemResponse"}).count() > 0){
        db.itemsessions.update(
            {"responses._t":"models.itemSession.StringItemResponse"},
            {"$set":{"responses.$._t":"org.corespring.qti.models.responses.StringResponse"}},
            {multi:true}
        )
    }
    db.itemsessions.update(
        {"responses._t":"models.itemSession.ArrayItemResponse"},
        {"$set":{"responses.$._t":"org.corespring.qti.models.responses.ArrayResponse"}},
        {multi:true}
    )
    while(db.itemsessions.find({"responses._t":"models.itemSession.ArrayItemResponse"}).count() > 0){
        db.itemsessions.update(
            {"responses._t":"models.itemSession.ArrayItemResponse"},
            {"$set":{"responses.$._t":"org.corespring.qti.models.responses.ArrayResponse"}},
            {multi:true}
        )
    }
}

function down(){
    //in the database, there are different documents with correct type hints. we don't want to modify those
}