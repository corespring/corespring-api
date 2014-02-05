function up() {
    var content = db.content.find({"contributorDetails.licenseType": "CC BY"});

    content.forEach(function(item) {
        if (item.data && item.data.files) {
            item.data.files.forEach(function(file) {
                item.contributorDetails.licenseType = "CC-BY";
                db.content.save(item);
            });
        }
    });

}

function down() {
    // nope.
}
