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
};

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