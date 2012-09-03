conn = new Mongo("localhost");
corespringLiveDb = conn.getDB("corespring-live");

var liveItems = corespringLiveDb.collection.find();


var apiDevDb = conn.getDB("corespring-api-dev");


liveItems.forEach(function(item){

    var newItem = {};
    newItem.id = item.id;
    newItem.name = item.name;
    newItem.description = item.description;
    newItem.isPrivate = false;

    apiDevDb.contentcolls.insert(newItem);
});

