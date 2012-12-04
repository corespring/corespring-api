var tree = [];


function findObjectWithName(tree,name, buildObjectFn, isItemFn, allFn ){

  if( tree.length == 0 )
  {
    tree.push(allFn())
  }
  
  if(name == undefined)
  {
    return { name: undefined, items: []};
  }
  for( var x = 0; x < tree.length; x++){
    var item = tree[x];

    //print("item: " + item + " name: " + name);
    if(isItemFn(item, name))
    {
      return item;
    }
  }
  var newObject = buildObjectFn( name );

  if( newObject.name == "" ){
    print("not adding item")
  } else {
    tree.push(newObject);
  }
  
  return newObject;

}

function buildNested(name){ return { name: name, items: []}};
function allFn() { return { name : "All", items: [] } };

function isItem(item, name){ return item.name == name};

db["ccstandards_new"].find().forEach(function(item){

  
  var subjectHolder = findObjectWithName(tree,item.subject, buildNested, isItem, allFn);

  var categoryHolder = findObjectWithName(subjectHolder.items, item.category, buildNested, isItem, allFn);

  var subCategoryHolder = findObjectWithName(categoryHolder.items, item.subCategory, function(name){ return name}, function(item, name){ return item == name}, function(){return "All"});

});

printjson(tree);
