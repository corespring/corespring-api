//expects vars from and to to have been set in an eval

if( !from || !to ){
    throw "You must specify from and to eg: --eval 'var from = \"dbone\"; var to = \"dbtwo\";'"
}
print("from db: " + from);
print("to db: " + to);

conn = new Mongo("localhost");

var fromDb = conn.getDB(from);
var toDb = conn.getDB(to);

print("copying subjects");
//Note: I'm renaming the collection to 'subjects' to make it consistent.
fromDb.subject.find().forEach( function(x){toDb.subjects.insert(x)} );


print("copying cc-standards");
fromDb["cc-standards"].find().forEach( function(x){toDb["ccstandards"].insert(x)} );

