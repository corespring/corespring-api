#!/usr/bin/env ruby

require 'json'
require 'time'
require 'mongo-db-utils/version'
require 'mongo-db-utils/tools/commands'
require 'mongo-db-utils/models/db'

require_relative '../../../libs/ruby/log'

required_version = "0.1.3"

raise "You need version #{required_version} of mongo-db-utils" if MongoDbUtils::VERSION != required_version

puts "using mongo-db-utils version: #{MongoDbUtils::VERSION}"

include MongoDbUtils::Tools
include MongoDbUtils::Model

log "-------------------------------------"
log "staging/push/before/copy_live_db.rb"
log "-------------------------------------"

raise "no config file specified" if ARGV[0] == nil

config = JSON.parse(IO.read(ARGV[0]))

live_db_uri = ENV["CORESPRING_LIVE_DB_URI"]
raise "no live db specified" if live_db_uri == nil

target_db_uri = config["ENV_MONGO_URI"]
raise "no target db specified" if target_db_uri == nil

# don't throw an error here - it'll be thrown in get_db if we need it
target_replica_set_name = config["ENV_MONGO_REPLICA_SET_NAME"]

# check the mongo_uri - if it contains commas assume its a replica set uri
# then check for a replica set name
# otherwise create a simple db
def get_db(uri, replica_set_name)
  if(uri.include?(","))
    raise "[URI Problem] -- we think this is a replica set uri: #{uri} - if it is we also need the replica set name." if replica_set_name.nil?
    ReplicaSetDb.new(uri, replica_set_name)
  else
    Db.new(uri)
  end
end


`mkdir -p tmp_folder`
raise "error making folder" unless $?.to_i == 0

log "running dump of #{live_db_uri}"

live_db = get_db( live_db_uri, ENV["CORESPRING_LIVE_DB_REPLICA_SET_NAME"] )

# Just print the command for now
Dump.new(live_db.to_host_s,
  live_db.name,
  "tmp_folder",
  live_db.username,
  live_db.password).run

target_db = get_db(target_db_uri, target_replica_set_name)

=begin
## Get the versions count
count_raw = `mongo #{live_db.first_host_port}/#{live_db.name} -u #{live_db.username} -p #{live_db.password} --eval "db.mongo_migrator_versions.count();"`
raise "error getting versions count" unless $?.to_i == 0
count = count_raw.chomp[-1]

if count == "0"
  log "deleting migrations - because they aren't going to get overrwritten"
  `mongo #{target_db.host}:#{target_db.port}/#{target_db.name} -u #{target_db.username} -p #{target_db.password} --eval "db.mongo_migrator_versions.drop();"`
  raise "error dropping versions" unless $?.to_i == 0
end
=end

# 2. send to the target db

# Note:
# We get this error when running a restore:
# Error creating index corespring-staging.system.usersassertion: 13111 field not found, expected type 2
# We catch this for now as the db has been restored, this is some form of indexing issue

log "call MongoTools.restore..."
begin
  puts Restore.new(
    target_db.to_host_s,
    target_db.name,
    "tmp_folder/#{live_db.name}",
    target_db.username,
    target_db.password).run
rescue ToolsException => mte
  log "MongoToolsException --------->"
  log mte.cmd
  log mte.output
  log "MongoToolsException ---------"
end

`rm -fr tmp_folder`
raise "error running rm" unless $?.to_i == 0
log "exit successfully..."



