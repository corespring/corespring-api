#!/usr/bin/env ruby
require 'json'

require_relative '../../../libs/ruby/db'
require_relative '../../../libs/ruby/mongo_tools'

puts "-------------------------------------"
puts "staging/push/before/copy_live_db.rb"
puts "-------------------------------------"

raise "no config file specified" if ARGV[0] == nil

config = JSON.parse(IO.read(ARGV[0]))

live_db_uri = ENV["CORESPRING_LIVE_DB_URI"]
raise "no live db specified" if live_db_uri == nil

target_db_uri = config["ENV_MONGO_URI"]
raise "no target db specified" if target_db_uri == nil



`mkdir -p tmp_folder`
raise "error making folder" unless $?.to_i == 0

puts "running dump of #{live_db_uri}"
live_db = Db.from_uri(live_db_uri)
# 1. dump the live db
MongoTools.dump(
  live_db.host, 
  live_db.port, 
  live_db.name, 
  "tmp_folder", 
  live_db.username, 
  live_db.password)

target_db = Db.from_uri(target_db_uri)

## Get the versions count
count_raw = `mongo #{live_db.host}:#{live_db.port}/#{live_db.name} -u #{live_db.username} -p #{live_db.password} --eval "db.mongo_migrator_versions.count();"`
raise "error getting versions count" unless $?.to_i == 0
count = count_raw.chomp[-1]

if count == "0"
  puts "deleting migrations - because they aren't going to get overrwritten"
  `mongo #{target_db.host}:#{target_db.port}/#{target_db.name} -u #{target_db.username} -p #{target_db.password} --eval "db.mongo_migrator_versions.drop();"`
  raise "error dropping versions" unless $?.to_i == 0
end




# 2. send to the target db

# Note: 
# We get this error when running a restore:
# Error creating index corespring-staging.system.usersassertion: 13111 field not found, expected type 2
# We catch this for now as the db has been restored, this is some form of indexing issue
begin
  MongoTools.restore(
    target_db.host, 
    target_db.port, 
    target_db.name, 
    "tmp_folder/#{live_db.name}", 
    target_db.username, 
    target_db.password) 
rescue MongoToolsException => mte
  puts "MongoToolsException --------->"
  puts mte.cmd
  puts mte.output
  puts "MongoToolsException ---------"
end

`rm -fr tmp_folder`
raise "error running rm" unless $?.to_i == 0
puts "exit successfully..."




