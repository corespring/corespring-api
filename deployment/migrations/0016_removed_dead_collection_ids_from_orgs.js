var collectionExists = function(id){
  return db.contentcolls.count({_id : id}) == 1;
}

function up(){

  db.orgs.find().forEach(function(org){

    print("cleaning org: " + org.name);
    var out = [];
    for(var i = 0; i < org.contentcolls.length; i++){

      var ref = org.contentcolls[i];
      var refId = ref.collectionId;
      if( collectionExists(refId) ){
        out.push(ref);
      } else {
        print("This collection doesn't exist: " + refId);
      }
    }

    org.contentcolls = out;
    //db.orgs.save(org);
  });
}

function down(){


}