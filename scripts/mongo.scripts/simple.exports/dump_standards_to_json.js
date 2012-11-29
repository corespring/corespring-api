//A little script that makes some json
//Note that you'll have to do a little bit of tidy up on the dump file
//Usage: mongo mongourl -u root -p password dump_standards_to_json.js > dump.json
print("[")
db.ccstandards.find().forEach( function(s){
    printjson(s)
    print(",")
})
print("]")
