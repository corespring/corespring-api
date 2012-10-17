//expects vars from and to to have been set in an eval

if( !from || !to ){
    throw "You must specify from and to eg: --eval 'var from = \"dbone\"; var to = \"dbtwo\";'"
}
print("from db: " + from);
print("to db: " + to);

conn = new Mongo("localhost");
corespringLiveDb = conn.getDB(from);

var apiDevDb = conn.getDB(to);


var liveItems = corespringLiveDb.templates.find();

liveItems.forEach(function (item) {
    var target = {};
    for (var x in item) {
        target[x] = item[x];
    }
    apiDevDb["templates"].insert(target);
});
