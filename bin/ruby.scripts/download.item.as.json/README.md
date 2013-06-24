# Download item to json

This utility downloads an item from the given corespring db and creates a json file in the conf/seed-data/dev folder.
It changes the collection id to point to the 'Items from Production' collection.

### Why?
So that developers can quickly pull a live item to the local environment to see if they can reproduce the issue.


### Usage

    ./run ${itemId} ${mongo_uri}


    //Then launch the local corespring app and find the item in the web console.
