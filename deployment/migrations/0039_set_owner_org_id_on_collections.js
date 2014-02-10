/**
 * Static reference of collection names to known ObjectIds. Used for reability in orgs reference.
 */
var collections = {
    "CoreSpring ELA": ObjectId("4ff2e4cae4b077b9e31689fd"),
    "CoreSpring Mathematics": ObjectId("4ff2e56fe4b077b9e3168a05"),
    "archiveColl": ObjectId("500ecfc1036471f538f24bdc"),
    "Beta Items (Public)": ObjectId("505777f5e4b05f7845735bc1"),
    "Demo Collection": ObjectId("51114b127fc1eaa866444647"),
    "LearnZillion": ObjectId("5162d9b9ac1f68b4461138d9")
};

/**
 * Static reference of orgs, their ids, and collections that we know they should own.
 */
var orgs = {
    CoreSpring: {
        id: ObjectId("502404dd0364dc35bb393398"),
        owns: [
            collections["CoreSpring ELA"],
            collections["CoreSpring Mathematics"],
            collections["archiveColl"],
            collections["Beta Items (Public)"],
            collections["Demo Collection"]
        ]
    },
    LearnZillion: {
        id: ObjectId("516ffdb6e4b0547bcd4ffdc0"),
        owns: [
            collections["LearnZillion"]
        ]
    }
};

/**
 * Returns true if the collection belongs to one organization, false otherwise
 */
function belongsToOneOrg(c) {
    return db.orgs.count({"contentcolls.collectionId": c._id}) == 1;
}

/**
 * Returns the ObjectId of the first found org related to the collection.
 */
function getOnlyOrgForCollection(c) {
    return db.orgs.find({"contentcolls.collectionId": c._id}).next()._id;
}

/**
 * Returns the statically available ObjectId of the org owning the collection
 * (should check belongsToOneOrg first).
 */
function getKnownOwnerOfCollection(inputC) {
    for (org in orgs) {
        if (orgs[org].owns.map(function(c) { return c.valueOf(); }).indexOf(inputC._id.valueOf()) >= 0) {
            return orgs[org].id;
        }
    }
    return undefined;
}

/**
 * Returns the ObjectID of the org which should own the collection
 */
function getOwnerId(collection) {
    if (belongsToOneOrg(collection)) {
        return getOnlyOrgForCollection(collection);
    } else {
        return getKnownOwnerOfCollection(collection);
    }
}

function up() {
    db.contentcolls.find().forEach(function(collection) {
        db.contentcolls.update({"_id": collection._id}, { $set: { "ownerOrgId": getOwnerId(collection) }});
    });
}

function down() {
    db.contentcolls.update({}, { $unset: {"ownerOrgId": ""} }, { multi: true });
}