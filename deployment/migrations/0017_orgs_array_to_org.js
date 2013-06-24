function up() {

  db.users.find().forEach(function (u) {

    if (u.orgs && u.orgs.length > 0) {
      print("updating: " + u.userName);
      u.org = u.orgs[0];
      delete u.orgs;
      delete u.hasRegisteredOrg;
      db.users.save(u);
    }
  });
}

function down() {

  db.users.find().forEach(function (u) {

    if (u.org) {
      u.orgs = [u.org];
      delete u.org;
      u.hasRegisteredOrg = true;
    } else {
      u.hasRegisteredOrg = false;
    }
    db.users.save(u);
  });
};