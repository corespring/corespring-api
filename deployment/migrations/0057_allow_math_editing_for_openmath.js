function process(showMath) {
  var openMathCollection = db.getCollection('contentcolls').findOne({name: "Open Math"});
  if (!openMathCollection) {
    print("Error: Can't find Open Math collection");
    return;
  }
  var collId = openMathCollection._id.valueOf();
  var num = 0;
  db.content.find({collectionId: collId}).forEach(function(item) {
    var components = item.playerDefinition && item.playerDefinition.components;
    for (var cKey in components) {
      var component = components[cKey];
      if (component.componentType == 'corespring-extended-text-entry') {
        component.model.config.showMathInput = showMath;
        db.content.save(item);
        num++;
      }
    }
  });
  print(num + " items chaned");
}

function up() {
  process(true);
}

function down() {
  process(false);
}
