var sIds = [];
db.subjects.find().forEach(function(s){
 sIds.push(s.id);
});

db.content.find().sort()