print("initial count: " + db.content.count());
var query = { gradeLevel: { $nin: ["KG","01","02","03","04","05","06","07","08"]}};
print( "items found: " + db.content.count(query));
db.content.remove(query);
print( "items found: " + db.content.count(query));

var secondQuery = {

  $and: [
    { gradeLevel:
      {
        $in: ["04","05", "06", "07", "08"]
      } },
    { "contributorDetails.author": "State of New Jersey Department of Education" }
  ]
};
print( "second query count: " + db.content.count(secondQuery));
db.content.remove(secondQuery);
print( "second query count: " + db.content.count(secondQuery));

print("final count: " + db.content.count());
