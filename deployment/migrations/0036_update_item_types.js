var mapping = {
    "Project": "Composite - Project",
    "Performance": "Composite - Performance",
    "Activity": "Composite - Activity"
};

function up() {
    var changeCount = 0;
    db.content.find().forEach(function(item) {
        var changed = false;
        for (k in mapping) {
            if (item.taskInfo && item.taskInfo.itemType == k) {
                item.taskInfo.itemType = mapping[k];
                changed = true;
            }
        }
        if (changed) {
            db.content.save(item);
            changeCount++;
        }
    });

    print("0036 - updated " + changeCount + " items");
}