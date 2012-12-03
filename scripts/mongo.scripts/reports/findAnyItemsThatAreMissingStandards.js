//findAnyItemsThatAreMissingStandards.js


var badItems = {};

db.content.find().forEach(function(item){

  var standards = item.standards;

  if(standards){
    for(var i = 0 ; i < standards.length ; i++ ){

      var s = standards[i];

      var count = db["ccstandards_new"].count( { _id : s });
      if(count === 0){
        badItems[item._id] = (badItems[item._id] || { standards: [], item: item});
        badItems[item._id].standards.push(s);
      }
    }
  }

});

print(badItems.length);
var count = 0;
for(var x in badItems){

  print(badItems[x].item._id)
  print(badItems[x].item.title)
  count++;
}

print(count);