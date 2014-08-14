// See CA-1946

// is valid item - looks like some are malformatted
function isValidItem(item) {
    if (item.matchedDocuments) {
        return item.matchedDocuments.every(function(doc) {
            return (doc.id && doc.id.version)
        });
    }
    return false;
}

// get list of items that need to be moved to versioned_content
function getItemsToMove(matchedDocs) {
    return matchedDocs.filter(function(doc) {
        return matchedDocs.some(function(someDoc) {
            return someDoc.id.version > doc.id.version
        });
    });
}

function moveItems(itemsToMove) {
    itemsToMove.forEach(function(item) {
        print("moving " + item.id._id + ":" + item.id.version);
        var dbItem = db.content.findOne({
            "_id._id": ObjectId(item.id._id.valueOf()),
            "_id.version": item.id.version
        });
        db.versioned_content.save(dbItem);
        db.content.remove({
            "_id._id": ObjectId(item.id._id.valueOf()),
            "_id.version": item.id.version
        });
    });
}

function up() {
    var csr = db.content.aggregate(
        [

            {
                $group: {
                    _id: {
                        _id: "$_id._id"
                    },
                    count: {
                        $sum: 1
                    },
                    matchedDocuments: {
                        '$push': {
                            'id': '$_id',
                            'desc': '$taskInfo.description',
                            'modified': '$dateModified'
                        }
                    }
                }
            }, {
                $match: {
                    count: {
                        $gte: 2
                    }
                }
            }
        ]
    );
    print("found " + csr.result.length + " items with more than one version in db");
    csr.result.forEach(function(item) {
        if (isValidItem(item)) {
            print("\n\nprocessing: " + item._id._id);
            var itemsToMove = getItemsToMove(item.matchedDocuments);
            print("need to move " + itemsToMove.length + " items");
            moveItems(itemsToMove);
        }
    });
}

