/*
   Some items appear to have components-data but no related entry in the xhtml. This makes the
   outcome processor fail.

   The script finds these items, and removes the component-data, which do not have a related
   entry in xhtml.

   Ed: The error is happening because the item components model has a component definition with
   id of: 'graphTest', but the session on which it's trying to generate an outcome doesn't have
   an answer for 'graphTest'
   https://gist.github.com/edeustace/b9d76a2df03d7a596de4
   So - we'll need to update the outcome processor to skip components that don't have an answer
   to create an outcome from, and return an empty object instead
*/

function RemoveStaleComponents(contentCollection, doUpdate){

  this.main = main;

  function main(){
    return getItems().map(function(item){
      var missingComps = [];
      var existingComps = [];
      for(var compId in item.playerDefinition.components){
        if(item.playerDefinition.xhtml.indexOf(compId) === -1){
          missingComps.push(compId);
        } else {
          existingComps.push(compId);
        }
      }
      var updatedComponents = copyExistingComps(item.playerDefinition.components, existingComps);
      if(doUpdate) {
        contentCollection.update(
          {_id: item._id},
          {$set: {"playerDefinition.components":updatedComponents}}
        );
      }
      return missingComps.length ?
      {
        item: item,
        updatedComponents: updatedComponents,
        missingComps:missingComps,
        existingComps:existingComps
      } : {
        item:item
      }
    });
  }

  function copyExistingComps(comps, existingCompIds){
    var result = {};
    for(var i = 0; i < existingCompIds.length; i++){
      result[existingCompIds[i]] = comps[existingCompIds[i]];
    }
    return result;
  }


  /**
   * These are the items, that have been manually edited
   * and potentially have this problem
   * @returns {*}
   */
  function getItems(){
    return contentCollection.find({"_id._id": {$in: [
      ObjectId("54cbdedbe4b02b2108ad0618"),
      ObjectId("54cbd8ade4b0efb5ed20f2d0"),
      ObjectId("545e3874e4b02ad5cbabb765"),
      ObjectId("545ea92de4b0d8b4eaff044a"),
      ObjectId("545e743be4b0878f0fac9fbb"),
      ObjectId("545d3dfce4b02744ca57bd75"),
      ObjectId("545d9639e4b0a74cd55d06d6"),
      ObjectId("545d9a22e4b02ad5cbabb717"),
      ObjectId("545d9d16e4b02ad5cbabb71a"),
      ObjectId("545d9fdce4b02744ca57bd99"),
      ObjectId("545da456e4b02ad5cbabb71d"),
      ObjectId("545da78be4b0878f0fac9f05"),
      ObjectId("545e287de4b0bb57da56ad94"),
      ObjectId("545e2bb1e4b0bb57da56ad95"),
      ObjectId("545e7d34e4b02744ca57be60"),
      ObjectId("545e836fe4b0e3e9f3e20114"),
      ObjectId("5454d6e0e4b074be1845dc37"),
      ObjectId("54cbb961e4b0efb5ed20e3a0"),
      ObjectId("5454f626e4b0c6a0be381a2d"),
      ObjectId("545ea052e4b0402543215198"),
      ObjectId("545e9ec9e4b0e3e9f3e20152"),
      ObjectId("545e9ba1e4b0e3e9f3e20143"),
      ObjectId("545e974de4b0d8b4eaff040e"),
      ObjectId("545e9173e4b0e3e9f3e20135"),
      ObjectId("545e901be4b0402543215171"),
      ObjectId("545e8d1fe4b0d8b4eaff03ff"),
      ObjectId("54cbc9787f9131a6c91ce836"),
      ObjectId("54cba602e4b02b2108ace68c"),
      ObjectId("54cbb177e4b0f323ceb53e50"),
      ObjectId("545e31d8e4b0878f0fac9f55"),
      ObjectId("545daa75e4b02744ca57bda5"),
      ObjectId("545dad7be4b02ad5cbabb729"),
      ObjectId("545db61ae4b0bb57da56ad73"),
      ObjectId("545db6f4e4b02744ca57bdbf"),
      ObjectId("545e6de6e4b02ad5cbabb7b2"),
      ObjectId("545e89e4e4b0b588e1c1b80b"),
      ObjectId("545e86b5e4b02744ca57be6e"),
      ObjectId("5453b5f4e4b02f751c8fb919")]}},
      {
        "_id._id": 1,
        "_id.version": 1,
        "playerDefinition.xhtml": 1,
        "playerDefinition.components": 1
      });
  }
}

//
var doUpdate = false; //set to true to write updates back to db
//new RemoveStaleComponents(db.content, doUpdate).main();
new RemoveStaleComponents(db.versioned_content, doUpdate).main();
