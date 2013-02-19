
function removeCollection(name){

  var collection = db.contentcolls.findOne({name: name});
  if(collection){
    var itemQuery = {collectionId: collection._id.toString()}
    print( "no of items for " + name + ": " + db.content.count(itemQuery));
    db.content.remove(itemQuery);
    db.contentcolls.remove({name: name});
  }
}

function moveItemsToAnotherCollection( itemQuery, newCollectionId ){
 db.content.find(itemQuery).forEach(function(i){
   i.collectionId = newCollectionId;
   db.content.save(i);
 });
}

function renameCollection(from, to){
  db.contentcolls.find({name: from}).forEach(function(c){
    c.name = to;
    db.contentcolls.save(c);
  });
}



removeCollection("mcas");
removeCollection("Rosemary");
removeCollection("ItemsforEd");


var testTitle = { "taskInfo.title": /^Test.*/ };
var testCollection = db.contentcolls.findOne({name: "Test Items"});

moveItemsToAnotherCollection(testTitle, testCollection._id.toString());

renameCollection("Rosemary's Discard Pile", "Discard");
renameCollection("Beta Items", "Beta Items (Public)");
