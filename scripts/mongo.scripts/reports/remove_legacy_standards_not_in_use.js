//remove_legacy_standards_not_in_use.js

var canRemove = 0;

print("before count: " + db.ccstandards_new.count());

function removeIfNotUsed(standard){

    var itemsThatUseStandardCount = db.content.count( {standards: standard._id });

    if(itemsThatUseStandardCount === 0){
        //print("safe to remove: " + standard.dotNotation);
        canRemove++;
        db.ccstandards_new.remove(standard);
    } else {
        print( itemsThatUseStandardCount + " items use this standard: " + standard.dotNotation)
    }

}


db.ccstandards_new.find({ source: "LegacyOnly" }).forEach( removeIfNotUsed );

print("can remove: " + canRemove);


print("sourceId,sourceDotNotation,sourceStandard,destinationUrl");

function printColumns(s){
    print(s._id + "," + s.dotNotation + ", \"" + s.standard + "\",?");
}

db.ccstandards_new.find({ source: "LegacyOnly" }).forEach( printColumns );
var count = db.ccstandards_new.count({ source: "LegacyOnly" })
print(count);

print("after count: " + db.ccstandards_new.count());
