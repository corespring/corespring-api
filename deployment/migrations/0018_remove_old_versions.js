function up(){


  print("before we begin there are: " + db.content.count() + " items");
  var updatedItems = [];

  printNonCurrent();

  db.content.remove({ "version.current" : false });

  printNonCurrent();

  var latestItems = {
    $or: [
      {"version.current" : true},
      {version: {$exists: false}}
    ]
  };

  db.content.find(latestItems).forEach( function(item){
    setItemToVersionZero(item);
  });

  db.content.find({"_id._id" : { $exists: true}}).forEach(function(item){
    db.content.remove({_id: item._id._id});
  });

  print("at the end we have: " + db.content.count() + " items");

  print("updating item sessions now...");
  db.itemsessions.find().forEach(updateItemIdInSession(db.itemsessions));
  db.itemsessionsPreview.find().forEach(updateItemIdInSession(db.itemsessionsPreview));
};

function updateItemIdInSession(collection){

  print("collection: " + collection);
  return function(session){

    var pointsToItem = db.content.count( {"_id._id" : session.itemId }) == 1;

    if(!pointsToItem) {
        collection.remove(session);
    } else {
      db.content.find( { "_id._id" : session.itemId }, { _id : 1} ).forEach(function(item){
          if( session.itemId.toString() == "[object bson_object]"){
            print("ignore : " + session._id);
          } else {
            session.itemId = item._id;
            printjson( { msg: "session new item id", data: session.itemId});
            collection.save(session);
          }
      });
    }
  }
}

function setItemToVersionZero(item){
    if( item._id.toString() == "[object bson_object]"){
      return;
    } else {
      var objectId = item._id;
      item._id = { _id : objectId, version: 0};
      delete item.version;
      //printjson(item._id);
      db.content.save(item);
    }
}

function printNonCurrent(){
  var nonCurrentItems = db.content.count({"version.current" : false});
  print("There are " + nonCurrentItems + " non current items in the db");
};

function down(){
  //this is a destructive migration - can't rollback
}

up();