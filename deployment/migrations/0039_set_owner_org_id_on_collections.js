function up() {
    var orgs = db.orgs.find();

    orgs.forEach(function(org) {
        var orgId = org._id;
        var colls = org.contentcolls;
        var collsOwnedByOrg = [];
        for(var i = 0; i < colls.length; i++) {
            var coll = colls[i];
            if (coll['pval'] == 3) {
                collsOwnedByOrg.push(coll.collectionId);
            }
        }

        db.contentcolls.update({"_id": {$in: collsOwnedByOrg} }, { $set: {"ownerOrgId": orgId} }, { multi: true });

    });
}