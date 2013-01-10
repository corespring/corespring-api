
var usersToUpdate = ["gwen","evan","ed","MathContent"];

for(var i = 0 ; i < usersToUpdate.length ; i++ ){
  var user = usersToUpdate[i];
  db.users.find({ userName: user}).forEach( function(dbUser){
    dbUser.hasRegisteredOrg = true;
    db.users.save(dbUser);
  });
}

db.users.find().forEach(printjson);
