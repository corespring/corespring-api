function changeCollStatus(enabledStatus) {
    var orgs = db.orgs.find();

    orgs.forEach(function(org) {
        var id = org._id;

        var colls = org.contentcolls;
        var new_colls = [];

        for(var i = 0; i < colls.length; i++) {
            var coll = colls[i];
            coll['enabled'] = enabledStatus;
            new_colls.push(coll);
        }

        db.orgs.update({"_id":id}, {$set:{"contentcolls":new_colls}});
    });
}

function up() {
    changeCollStatus(true)
}

function down() {
    changeCollStatus(false)
}