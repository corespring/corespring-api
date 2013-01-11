function applyPVal(o){

  if(o.contentcolls){
    for(var i = 0 ; i < o.contentcolls.length; i++){
      if(!o.contentcolls[i].pval){
        o.contentcolls[i].pval = NumberLong("3");
        db.orgs.save(o);
      }
    }
  }
}

db.orgs.find({name: "Root Org"}).forEach(applyPVal);