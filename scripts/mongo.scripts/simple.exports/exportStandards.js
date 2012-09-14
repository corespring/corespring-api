//exportStandards.js

print("[")
db["cc-standards"].find().forEach( function(s){ printjson(s); print(",")})
print("]")