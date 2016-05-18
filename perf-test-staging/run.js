const minimist = require('minimist');
const MongoClient = require('mongodb').MongoClient;
const ObjectID = require('mongodb').ObjectID;
const _ = require('lodash');
const request = require('superagent');
let args = minimist(process.argv);

let uri = args.db;
let limit = args.limit || 1;
let orgId = args.orgId;
let host = args.host || 'http://localhost:9000';
let ids = args.ids ? require(args.ids) : null;

console.log(uri, limit, orgId);

if (!orgId || !uri) {
  console.warn('no orgId or uri');
  process.exit(1);
  return;
}



let loadMultipleScores = (ids, c) => {


  return new Promise((resolve, reject) => {
    let start = new Date();
    let params = { sessionIds: ids };
    console.log(JSON.stringify(params));
    let url = host + c.path + '?access_token=' + c.token;
    console.log('> start...', url);

    request
      .post(url)
      .send(params)
      .set('Accept', 'application/json')
      .end(function (err, res) {
        console.log('> done!', url);
        let duration = new Date().getTime() - start.getTime();
        console.log('> duration: ', duration);
        if (err || !res.ok) {
          reject(err);
        } else {
          resolve({ body: res.body, duration: duration });
        }
      });
  });
};

MongoClient.connect(uri, (err, db) => {

  let getAccessToken = (orgId) => {
    return new Promise((resolve, reject) => {
      let collection = db.collection('accessTokens');
      collection.findOne({ organization: new ObjectID.createFromHexString(orgId) }, function (err, t) {
        if (err) {
          reject(err);
        } else {
          let now = new Date();
          if (t.expirationDate > now) {
            resolve(t);
          } else {
            let then = new Date(now.setHours(now.getHours() + 1)); //.toISOString();
            collection.update({ _id: t._id }, { $set: { 'expirationDate': then } }, function (err, results) {
              if (err) {
                reject(err);
              } else {
                resolve(t);
              }
            });
          }
        }
      });
    });
  };

  let loadIds = () => {
    if (ids) {
      return Promise.resolve(ids.sessionIds);
    } else {
      return new Promise((resolve, reject) => {
        db.collection('v2.itemSessions').find(
          { components: { $exists: true }, 'identity.orgId': orgId },
          { '_id': 1 },
          { sort: [['_id', 'desc']], limit: limit }).toArray((err, arr) => {

            if (err) {
              reject(err);
            } else {
              console.log('arr: ', arr);
              let ids = _.map(arr, '_id');
              resolve(ids);
            }
          });
      });
    }
  };
  
  Promise.all([loadIds(), getAccessToken(orgId)])
   .then((results) => {
     
     let [ids, token] = results;
     console.log('ids...', ids);
     let p1 = loadMultipleScores(ids, { method: 'POST', path: '/api/v2/sessions/multiple-scores.json', token: token.tokenId });

      return p1.then((oneResult) => {
        return loadMultipleScores(ids, { method: 'POST', path: '/api/v2/sessions/multiple-scores-two.json', token: token.tokenId })
          .then((twoResult) => {
            return Promise.resolve({ one: oneResult, two: twoResult });
          });
      });
     
   }).then((r) => {
    //console.log('success: ', JSON.stringify(r, null, ' '));
    console.log('one duration: ', r.one.duration);
    console.log('two duration: ', r.two.duration);
    db.close();
    process.exit(0);
  }).catch((e) => {
    console.log('error', e, e.stack);
    db.close();
    process.exit(1);
  });

});