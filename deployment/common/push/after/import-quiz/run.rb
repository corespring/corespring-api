#!/usr/bin/env ruby

require 'json'
require_relative '../../../../libs/ruby/db'

puts "Db: #{Db}"
require_relative '../../../../libs/ruby/mongo_tools'
puts "MongoTools: #{MongoTools}"

puts "-------------------------------------"
puts "common/push/after/import-quiz/run.rb"
puts "-------------------------------------"
puts ""

raise "no config file specified" if ARGV[0] == nil

config = JSON.parse(IO.read(ARGV[0]))

target_db_uri = config["ENV_MONGO_URI"]
raise "no target db specified" if target_db_uri == nil

cmd = "./deployment/common/push/after/import-quiz/import.rb #{target_db_uri} dump"
puts "command: "
puts "#{cmd}"
`#{cmd}`
puts ""
puts "-------------------------------------"




