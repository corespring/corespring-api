print("[")
db.ccstandards.find().forEach( function(s){
    printjson(s)
    print(",")
})
print("]")
