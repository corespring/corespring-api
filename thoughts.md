# New player ==> corespring api thoughts

## Qti Markup -> Component models

We have a large number of items that define themselves through QTI. To support these in the new player, we will provide mappings from qti xml -> (xhtml + component json).

## ItemSessions  // New session format

The new item sessions will be different from the existing sessions and getting true bi-directional mapping will be a bit of work. For that reason we are going to keep the 2 session formats separate and not inter-operable. This means that a session from the v1 player can only be viewed in the v1 player - not in the v2 player (and vice versa)

## Routing requests

### Launching by item id

This is quite straightforward, we keep the existing player endpoint for the current player and we add a new endpoint for the new player.

    GET /player/:itemId                 //launches existing player by item id
    GET /player/:playerVersion/:itemId  //launches player with `playerVersion` by item id

The current player is going to have a version of *1.0*, so calling `GET /player/1.0/x` is the same as `GET /player/x`

The new player will be version 2.0.

### Player versioning and compatibility

- support minor version backward compatibility? libraries will need to be namespaced? players as their own app servers?

### Launching by session id

Sessions between the players are not interchangeable. A session id for the 2.0 player, will only ever run that player.

    GET /player/:sessionId  //launch player by session id.

The flow here is: 

* Find sessionId in v1 itemsessions collection if found launch the 1.0 player
** If not found
*** Find session id in 2.0 item session collection, if found launch 2.0 player
*** If not found => 404

#### Launch by session id + player version

If you know the player version you can specify it in the path: 

    GET /player/:playerVersion/:sessionId


