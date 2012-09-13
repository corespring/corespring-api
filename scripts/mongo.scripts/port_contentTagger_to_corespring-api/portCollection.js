//expects vars from and to to have been set in an eval

if( !from || !to ){
    throw "You must specify from and to eg: --eval 'var from = \"dbone\"; var to = \"dbtwo\";'"
}
print("from db: " + from);
print("to db: " + to);

conn = new Mongo("localhost");
corespringLiveDb = conn.getDB(from);

var liveItems = corespringLiveDb.collection.find();


var apiDevDb = conn.getDB(to);


liveItems.forEach(function(item){

    //print("source item Id: " + item._id);
    var newItem = {};
    newItem._id = item._id;
    newItem.name = item.name;
    newItem.description = item.description;
    newItem.isPrivate = false;
    //print("new item id: " + newItem._id);
    apiDevDb.contentcolls.insert(newItem);
});

