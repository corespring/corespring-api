#! /bin/bash

echo "importing staging database"

echo "removing temp prod_db"
rm -rf ~/prod_db

echo "importing staging database in binary form"
mongodump --host ds063347-a1.mongolab.com --port 63347 --db corespring-staging --username corespring --password cccrcId4d --out ~/prod_db

echo "removing database prod_db"
rm -rf ~/data/prod_db

echo "recreating database prod_db"
mkdir ~/data/prod_db

echo "restoring staging data into database prod_db"
mongorestore --dbpath ~/data/prod_db ~/prod_db

echo "running database on forked process"
mongod --dbpath ~/data/prod_db &

while ! [ exec 6<>/dev/tcp/127.0.0.1/27017 ]; do echo "waiting for 27017"; sleep 1; done

echo "copying corespring-staging db to api db"
mongo --eval "db.copyDatabase('corespring-staging', 'api')"

echo "killing forked mongo process"
kill -9 $!
if [ $? = 0 ]; then
	rm -rf ~/prod_db
	echo "all done, you may now run 'mongod --dbpath ~/data/prod_db' to use staging data locally"
else
	echo "an error occurred"
fi
