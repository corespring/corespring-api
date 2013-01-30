#!/usr/bin/env ruby
require 'json'

puts "-----------------------"
puts "back up db"
puts "-----------------------"

raise "no config file specified" if ARGV[0] == nil

config = JSON.parse(IO.read(ARGV[0]))

mongo_uri = config["ENV_MONGO_URI"]

raise "can't find ENV_MONGO_URI in config" if mongo_uri == nil

bucket_name = ENV["CORESPRING_AMAZON_BUCKET"]
access_key = ENV["CORESPRING_AMAZON_ACCESS_KEY"]
secret_key = ENV["CORESPRING_AMAZON_SECRET_KEY"]

error_msg = <<eos
missing some variables! check that you have the following configured:
CORESPRING_AMAZON_BUCKET
CORESPRING_AMAZON_ACCESS_KEY
CORESPRING_AMAZON_SECRET_KEY
eos

raise error_msg if (bucket_name == nil || access_key == nil || secret_key == nil)

cmd = "mongo-db-utils backup_s3 #{mongo_uri} #{bucket_name} #{access_key} #{secret_key}"

puts "running: [#{cmd}]"
`#{cmd}`

exit_code = $?.to_i
puts "exit code: #{exit_code}"

raise "an error occurred running this script: #{cmd}" unless exit_code == 0
