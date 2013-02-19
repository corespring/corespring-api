/**
 * Moves any item in contentDb that isn't in liveDb to a new staging collection
 * That people don't have public access to
 * expects vars live and content content have been set in an eval
 *
 */
if (!live || !content) {
  throw "You must specify live and content eg: --eval 'var live = \"dbone\"; var content = \"dbtwo\";'"
}
print("live db: " + live);
print("content db: " + content);

conn = new Mongo("localhost");
var liveDb = conn.getDB(live);
var contentDb = conn.getDB(content);

var publicCollectionIds = [];
var collectionNames = [];
var publicCollectionIdStrings = [];

var namesToCopy = ["CoreSpring Mathematics", "CoreSpring ELA", "Beta Items"];

contentDb.contentcolls.find({ name: {$in: namesToCopy}}).forEach(function (c) {
  publicCollectionIds.push(c._id);
  publicCollectionIdStrings.push(c._id.toString());
  collectionNames.push(c.name);
});


var liveDbIds = [];

liveDb.content.find({}, {}).forEach(function (item) {
  liveDbIds.push(item._id);
});


var itemsNotLiveAndInPublicQuery = {
  _id: { $nin: liveDbIds },
  collectionId: {$in: publicCollectionIdStrings }
};

function getOrCreateStagingCollection(cId) {

  if (!cId) {
    return null;
  }

  var currentCollection = contentDb.contentcolls.findOne({ _id: ObjectId(cId)});
  currentCollection.isPublic = true;
  contentDb.contentcolls.save(currentCollection);

  print("content_colls count: " + contentDb.contentcolls.count());
  var query = { name: currentCollection.name, isPublic: false };

  var count = contentDb.contentcolls.count(query);

  if (count == 0) {

    var newCollection = {};
    newCollection.name = currentCollection.name;
    newCollection.description = currentCollection.description;
    newCollection.isPublic = false;
    newCollection._id = new ObjectId();
    contentDb.contentcolls.insert(newCollection);
    print(">> new >>");
    printjson(newCollection);
    return newCollection;
  }
  else {
    var out = contentDb.contentcolls.findOne(query);
    print("from db");
    printjson(out);
    return out;
  }

}

function moveItemsToStagingCollections(item) {
  var stagingCollection = getOrCreateStagingCollection(item.collectionId);
  if (stagingCollection) {
    print("move to: " + stagingCollection.name);
    item.collectionId = stagingCollection._id.toString();
    contentDb.content.save(item);
  }
}

var noOfItemsToProcess = contentDb.content.count(itemsNotLiveAndInPublicQuery);
print("total no: " + contentDb.content.count());
print("no of items: " + noOfItemsToProcess);
contentDb.content.find(itemsNotLiveAndInPublicQuery).forEach(moveItemsToStagingCollections);

contentDb.contentcolls.find({ name: {$in: collectionNames}, isPublic: false}).forEach(function (item) {
  var count = contentDb.content.count({collectionId: item._id.toString()});
  print(count + " items in " + item.name);
});


//Add collections to the root org
function addStagingCollectionsToOrg(org) {

  function findById(arr, id) {
    for (var x = 0; x < arr.length; x++) {
      var item = arr[x];
      if (item.collectionId.toString() == id.toString()) {
        return item;
      }
    }
    return null;
  }

  contentDb.contentcolls.find({ name: {$in: collectionNames }, isPublic: false}).forEach(function (stagingColl) {

    if (findById(org.contentcolls, stagingColl._id) == null) {
      org.contentcolls.push({
        collectionId: stagingColl._id,
        pval: NumberLong(3)
      });
    }
  });
  contentDb.orgs.save(org);
}

contentDb.orgs.find({name: "Root Org"}).forEach(addStagingCollectionsToOrg);


contentDb.contentcolls.find({name: {$in: collectionNames}, isPublic: true}).forEach(function(pc){
  pc.name = pc.name + " (Public)";
  contentDb.contentcolls.save(pc);
});
