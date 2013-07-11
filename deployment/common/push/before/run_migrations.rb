#!/usr/bin/env ruby
require 'json'

puts "-------------------------------------"
puts "common/push/before/run.rb"
puts "running db migrations"
puts "-------------------------------------"

raise "no config file specified" if ARGV[0] == nil

config = JSON.parse(IO.read(ARGV[0]))

# The mongo migrator accepts 2 possible uri formats:
# 1. a regular mongo uri: mongodb://.....
# 2. a replica set name, a | and then a mongo uri eg: set-name|mongodb://....
# See: github.com/corespring/mongo-migrator for more information
def get_migrator_uri(uri, replica_set_name)
  if(uri.include?(","))
    raise "[URI Problem] -- we think this is a replica set uri: #{uri} - if it is we also need the replica set name." if replica_set_name.nil?
    "#{replica_set_name}|#{uri}"
  else
    uri
  end
end

JAR="deployment/libs/mongo-migrator_2.9.2-0.2.0-one-jar.jar"
MIGRATIONS="deployment/migrations"
MONGO_URI = config["ENV_MONGO_URI"]
REPLICA_SET_NAME = config["ENV_MONGO_REPLICA_SET_NAME"]
raise "Missing mongo uri" if MONGO_URI == nil

uri = get_migrator_uri(MONGO_URI, REPLICA_SET_NAME)

COMMIT_HASH = `git rev-parse --short HEAD`.chomp
# we might be using a pipe character for the migrator eg | - so make it a string
cmd = "java -jar #{JAR} migrate #{COMMIT_HASH} \"#{uri}\" #{MIGRATIONS}"
puts "command: "
puts "#{cmd}"
`#{cmd}`
exit_code = $?.to_i
puts "--------------> exit_code: #{exit_code}"
raise "An error occured running the script: #{cmd}" unless exit_code == 0