var someSecrets = [
  "aginf9gfi3gygdrmjl5we5p0b",
  "euo3cs4h5ychx15t7mfxvlfoh",
  "czfk8to7sufeg2xkjf2tx7bbh",
  "ar7l3edlwyeko4oidq153s4wz",
  "bvn0caxke6twclajcay9hbeot",
  "9h5081i3lb5x1h69m0b12sydv",
  "clwe4gs8bok1wqpiix8yetr0n",
  "djiocvb6cqp6livtdf1q9ttu7",
  "8dcba1p7ve05fyngmplwzkfdd",
  "9oewify198iitpumzaby1l131",
  "c74xyutzmx62yrhhrhl5azd4h",
  "f2tftxqm1bl64c6mvjb0ceftj",
  "7wv2wp2hxwlj4dtxum58o3j85",
  "aklzxv0n9jqzcdstfnubweott",
  "a6deuq72g4r1frlotznll4055",
  "asj6xl3yw7rnbpz4gkbqdw1z5",
  "8l7kuatb2y61rygzw48ay7wab",
  "dqsuarnm17008jdai7c4bpbuh",
  "d8c2l8po85o4kfyh97832i691",
  "8gn63x4zyrmo77gpirk3tvc3b",
  "e0i2248ilgionu02mv77p2qsv",
  "dy73rfng1zjpss50o3apl6ha7",
  "apeyk0jlbbq8bg1i98hylfiyp",
  "cf64r6bgdzyw44fh476c86agv",
  "7rrtz0bm685yt4hxepjvpo345",
  "byqhku279mhjfe3zoejsuug7h",
  "dttg2hmvdm4m7ln1z5z3eiaqp",
  "9oolm8cjvhb623ik2ji2j7bgd",
  "9swcexxsn99fhkjlldkqfjd57",
  "eh0ftzawlc7zkskcq9pbwcfph",
  "e43311om6mw3puy2sonxzn8oj",
  "cjzmkmzybidxpy3zh61wd4k3j"
];

var index = 0;

function getIndex(){
  index++;
  return index % someSecrets.length;
}

function up() {

  print("apiClients: " + db.apiClients.count());
  var query = {
    clientSecret: {
      $exists: true
    },
    $where: "this.clientSecret.length < 24"
  }

  db.apiClients.find(query).forEach(function (client) {

    db.orgs.find( {_id : client.orgId}).forEach(function(org){
      if(org){
        print( "Updating client secret for: " + org.name);
        print("clientId: " + client.clientId);
      }
    });
    client.clientSecret = someSecrets[getIndex()];
    print("new secret: " + client.clientSecret);
    db.apiClients.save(client);
  })
}

function down() {
 //one way migration
}

