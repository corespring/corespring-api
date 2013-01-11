print("initial count: " + db.content.count({title: ""}));
db.content.remove( { title:""});
print("final count: " + db.content.count({title: ""}));
