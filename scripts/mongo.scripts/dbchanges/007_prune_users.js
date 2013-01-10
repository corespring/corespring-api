var usersToPrune = ["ellen", "bburton"];

print("before: " + db.users.count());
db.users.remove({userName: { $in: usersToPrune}});
print("after: " + db.users.count());
