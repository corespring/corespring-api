db.contentcolls.update( { isPrivate: { $exists: true } }, {$unset: { isPrivate : 1 } }, false, true);


db.contentcolls.update(
  {
    name: {
      $in: ["CoreSpring Mathematics", "CoreSpring ELA"]
    }
  },
  {
    $set:
        { isPublic : true }
  },
  false,
  true
  );


