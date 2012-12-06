function getStandard(oid){

  try{
    return db.ccstandards_new.findOne( { _id: oid});
  }
  catch (e){
    return null;
  }
}

function moveStandardsFromOidToDotNotation(item){

  if(item.standards){
    var newStandards = [];

    for(var i = 0; i < item.standards.length; i++){
      var oid = item.standards[i];

      if(typeof(oid) === "string"){
        newStandards.push(oid);
      } else {
        var s = getStandard(oid);

        if(s){
          newStandards.push(s.dotNotation);
        }
      }
    }

    item.standards = newStandards;
    print("new standards: " + item.standards);
    db.content.save(item);
  }
}
var query = {};
print(db.content.count(query));
db.content.find(query).forEach(moveStandardsFromOidToDotNotation);