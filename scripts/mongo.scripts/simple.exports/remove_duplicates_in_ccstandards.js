//A little script that makes some json
//Note that you'll have to do a little bit of tidy up on the dump file
//Usage: mongo mongourl -u root -p password dump_standards_to_json.js > dump.json
var uniqueItems = {};
var duplicateItems = {};

var total = db[standardCollection].count();

print("total items: " + total);

if( !standardCollection ){
    throw "you need to specify a standardCollection eg: '--eval var standarCollection ...'";
}

db[standardCollection].find({uri: ""}).forEach( function(s){

    var count = db[standardCollection].count(
        {  standard: s.standard,
           category: s.category,
           subCategory: s.subCategory }
        );

    if( count == 1){
        uniqueItems[s._id] = s;
    }
    if(count >= 2){
        duplicateItems[s._id] = { count: count, standard: s };
    }

});

function count(o){
    var c = 0;
    for(var x in o){
        c++;
    }
    return c;
}

function reportDuplicates(){
    for(var key in duplicateItems){
        var s = duplicateItems[key];

        var usingBadStandardQuery = { standards: { "$in": [s.standard._id] }};
        var itemsUsingOldStandard = db.content.count( usingBadStandardQuery );
        if(itemsUsingOldStandard > 0){
            print("id: " + s.standard._id );
            print( "no of duplicates: " + s.count + ", " + s.standard.dotNotation +", standard: " + s.standard.standard);
            print( "items using standard: " + itemsUsingOldStandard);
        }
    }
}

print("Found " + count(uniqueItems) + " unique items");
print("Found " + count(duplicateItems) + " duplicate items");

reportDuplicates();

