# todos

## v1 api support

Most of the v1 api endpoints have been stripped out, with the exception of:
- collections
- items
- contributors - auth needed on api endpoint

1. check that the app still runs
2. check the app logs to see if there are any other calls still in use
3. urge any 3rd parties that are using v1 api calls to move to v2

### CollectionApi
All the collection calls now go through to v2 CollectionApi.
Filtering and skipping etc is disabled - I don't think we need it - but check the logs to be sure.

### ItemApi
The item calls need to be checked that they are still working
- resource api calls

- check w/ ev about motion math

- who/where is POST /api/v1/items being called - this shouldn't be running anymore.
in the last 30 days there were 34 calls to it..

### CMS and ItemApi

Do we update the cms to use v2 api calls instead? or do we proxy the calls

1. proxy the calls
2. update the client
3. remove proxy calls

Failing links:
~~http://localhost:9000/api/v1/metadata/item/50ee136ee4b00448c0368e0e:0~~
~~http://localhost:9000/api/v1/field_values/domain~~
~~http://localhost:9000/api/v1/items/50ee136ee4b00448c0368e0e:0~~

### Error - This is not the current version of this item (v1 editor)

Open the v1 editor and click the interaction tab and you should see the warning.


#### v1 item api search -> v2 search interface

Check if this is doable w/ ben

ben says it's tricky .. so the options are maintain v1 search in it's own module, or do the mapping

queries from log entries: 

path=/.*api\/v1\/collections.*&access_token=(.*)/ ==> 0 results
path=/.*api\/v1\/organizations.*&access_token=(.*)/ ==> 0 results
path=/.*api\/v1\/users.*&access_token=(.*)/ ==> 0 results
path=/.*api\/v1\/assessments.*&access_token=(.*)/ ==> 0 results
path=/.*api\/v1\/items.*&access_token=(.*)/ ==> lots of results results
path=/.*api\/v1\/items\/query.*/ ==> 0


TODO - find out who is using v1 search and get them to try v2

In the last 24 hours of 30.9.15 @ 16.26 GMT Motion Math made 253 search requests.

## sharing/unsharing

Alot of this logic has moved from the controller to ContentCollectionService. Add tests.

## reports

Reports is disabled

## find the todos in the code

Search for: `TODO: RF`

## add tests

Moving the business logic out has shown that we are missing alot of tests for the core business logic. Add some.
Search for `in pending` in the project.


~~## indexing needs to be re-hooked into itemService save~~

* extend the main ItemService and hook it in there?

## /developer/... routes 

They don't really work on prod - can we remove them? get requirements from ev
Same goes for /signup/... /reset/... and /auth/...


## supporting materials editor $on('enterEditor') isn't being fired and urls isn't set - also on develop branch

> Need to look at querying for standards and subjects
