/**
 * Mongo Script
 * Adds a demo access token that points to a demo org/user that has access to subset of collections.
 */

//first remove any old ones
db.orgs.remove({name: "Demo Org"});
db.accessTokens.remove({name: "demo_token"});
db.users.remove({userName: "demo_user"});

var demoUser = {
  "_id": ObjectId("50e5de27c41af3f2efd5fa91"),
  "provider": "userpass",
  "userName": "demo_user",
  "fullName": "Leeroy Jenkins",
  "email": "dev@null.com",
  "password": "$2a$10$ztrohKSsxniXnLsTJyAJVOlye5DFYlMi3GUQH1SHkVSO5IH.E9O72",
  "orgs": [
    {
      "orgId": ObjectId("502404dd0364dc35bb393397"),
      "pval": NumberLong("1")
    }
  ],
  "hasRegisteredOrg": true};

var demoToken = {
  "organization": ObjectId("502404dd0364dc35bb393397"),
  "tokenId": "demo_token",
  "scope": "demo_user"};

var demoOrg = {
  "name": "Demo Org",
  "path": [ ObjectId("502404dd0364dc35bb393397") ],
  "contentcolls": [ ],
  "_id": ObjectId("502404dd0364dc35bb393397")
};
var defaultPVal = NumberLong("9223372036854775807");

var collectionThatAreAccessible = ["CoreSpring ELA", "CoreSpring Mathematics", "Beta Items"];

function grantAccessToDemoOrg(c) {
  demoOrg.contentcolls.push({ collectionId: c._id, pval: defaultPVal });
  print(demoOrg.contentcolls);
}

db.contentcolls.find({ name: { $in: collectionThatAreAccessible}}).forEach(grantAccessToDemoOrg);
db.orgs.save(demoOrg);
db.accessTokens.save(demoToken);
db.users.save(demoUser);
