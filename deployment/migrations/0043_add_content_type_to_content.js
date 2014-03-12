function up() {
    db.content.find({'contentType': {$exists: false}}).forEach(function(item) {
        item.contentType = 'item';
        db.content.save(item);
    });
}

function down() {
    // nope.
}