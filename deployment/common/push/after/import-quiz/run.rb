#!/usr/bin/env ruby
require 'json'

require_relative '../../../../libs/ruby/db'
require_relative '../../../../libs/ruby/mongo_tools'

puts "-------------------------------------"
puts "common/push/after/import-quiz/run.rb"
puts "-------------------------------------"

raise "no config file specified" if ARGV[0] == nil

config = JSON.parse(IO.read(ARGV[0]))

puts "_____________XXXXxx________Xxx_______________________"


target_db_uri = config["ENV_MONGO_URI"]
raise "no target db specified" if target_db_uri == nil