/**
- Find any v2 item session that has an item id that is missing the version number.
- Aggregate the item ids to see which items are affected.
- Print information about the items, the collection and the org.
*/
var coll = db['v2.itemSessions'];

function gatherData(itemId){
  var item = db.content.findOne({'_id._id' : new ObjectId(itemId)});
  
  if(item){
    var collection = db.contentcolls.findOne(new ObjectId(item.collectionId));

    if(collection){
      var org = db.orgs.findOne(collection.ownerOrgId);
      return {item: item, org: org || {}, collection: collection};
    }
  }
}

function updateSessions(badItemId, fixedItemId){

  print('update sessions with an item id of: ', badItemId, 'to', fixedItemId);

  /*coll.update(
    {itemId: badItemId}, 
    {$set: { itemId: fixedItemId}}, 
    {
     upsert: false,
     multi: true,
   });*/
}

function aggregateItemIds(){
  var result = coll.aggregate([
    {$match: noVersionItemId}, 
    {$group: {_id: '$itemId', sessions: {$sum: 1}}, }
    ]);

  print('aggregated itemIds found in the sessions found in sessions with bad item ids....');

  var count = 0;
  while (result.hasNext()) {

    count += 1;
    var o = result.next();
    print(' ');
    print('itemid: ', o._id, 'no of sessions: ', o.sessions);

    var data = gatherData(o._id);

    if(data) {
      print('item:', data.item.taskInfo.title);
      print('collection:', data.collection.name);
      print('org:', data.org.name);
      //printjson(data);
    }
    var fixedItemId = data.item._id._id.valueOf() + ':' + data.item._id.version;
    updateSessions(o._id, fixedItemId);
  }

  print('found', count, 'bad item ids');
}

function aggregateIdentities(){
  
  var affectedIdentities = coll.aggregate([
    {$match: noVersionItemId}, 
    {$group: {_id: '$identity.orgId', sessions: {$sum: 1}}, }
    ]);

  print('aggregated identities found in the sessions found in sessions with bad item ids....');
  
  while(affectedIdentities.hasNext()){
    var id = affectedIdentities.next();

    print(' ');
    print('orgId: ', id._id, 'no of sessions', id.sessions);
    try{
      var orgId = new ObjectId(id._id);
      var org = db.orgs.findOne(orgId);
      if(org){
        print(org.name);
      }
    } catch (e) {
      printjson(e);
    }
  }
}

var all = coll.count();

var noVersionItemId = { itemId: /^[a-zA-Z0-9]*$/};
var noVersion = coll.count(noVersionItemId);
print('total sessions: ', all, 'sessions with an itemId with no version:', noVersion);
print('percent of all sessions:', noVersion / all, '%');

if(noVersion !== 0){
  aggregateItemIds();
  aggregateIdentities();
}
