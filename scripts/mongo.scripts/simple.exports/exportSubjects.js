//exportSubjects.js

print("[")
db.subjects.find().forEach( function(s){ printjson(s); print(",")})
print("]")