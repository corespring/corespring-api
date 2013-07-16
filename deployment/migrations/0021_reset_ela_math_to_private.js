function up() {
    var count = 0;

    // get the mongo ids of collections we want to remove from orgs
    var collsToRemove = [];
    db.contentcolls.find().forEach(function (coll) {
        count++;
        if ( (coll.name == "CoreSpring ELA (Public)") || (coll.name == "CoreSpring Mathematics (Public)"))  {
            collsToRemove.push(coll._id.toString())
            coll.isPublic = false;
            db.contentcolls.save(coll);
        }

    });


    db.orgs.find().forEach(function(org){
        // remove collsToRemove
        var colls = org.contentcolls;
        var filtered = colls.filter(function(c)
        {
            print(c.pval)
            if (collsToRemove.indexOf(c.collectionId.toString())  != -1) {
                return c.pval.toString() == "NumberLong(3)"
            } else {
                return true;
            }
        });

        org.contentcolls = filtered;
        db.orgs.save(org)


    });

}
