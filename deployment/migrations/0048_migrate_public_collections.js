// See CA-816
function up() {

    var corespringOrgId = "502404dd0364dc35bb393398";
    var migrations =
      [
        {
          "name": "Open Math",
          "ids": ["4ff2e56fe4b077b9e3168a05", "5114c77af4537b76ccc9a9ad"]
        },
        {
          "name": "Open ELA",
          "ids": ["4ff2e4cae4b077b9e31689fd", "5114c77af4537b76ccc9a9ae", "52da7506e4b0810cbac167cd"]
        },
        {
          "name": "Open Science",
          "ids": ["4ff5abe2e4b0e3bfeb9d2011"]
        }
      ];


    migrations.forEach(function(migration) {
        // create the collection if doesn't exist
        var newCollection = db.contentcolls.findOne({"name": migration.name, "ownerOrgId": ObjectId(corespringOrgId)});
        if (!newCollection) {
          newCollection =
          {
            _id: ObjectId(),
            isPublic: true,
            name: migration.name,
            ownerOrgId: ObjectId(corespringOrgId)
          };
          db.contentcolls.insert(newCollection);
        }
        print("migrating " +  migration.name + " " + db.content.find({"collectionId": { $in: migration.ids} }).count() + " items");
        db.content.update({"collectionId": { $in: migration.ids} }, { $set: {collectionId: newCollection._id.str } }, {multi: true});

        // add the collection to corespring org
        var contentCollRef =
        {
                "collectionId" : newCollection._id,
                "pval" : NumberLong(3),
                "enabled" : true
        };
        db.orgs.update({_id: ObjectId(corespringOrgId)}, { $push: { contentcolls: contentCollRef} });

    });


}

up();