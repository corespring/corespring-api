#!/usr/bin/env ruby

puts "------------------"
puts "which ruby?"
puts `which ruby`

puts "gem list?"
puts `gem list`
puts "------------------"
require 'json'
puts "json: #{JSON}"
require_relative '../../../../libs/ruby/db'

puts "Db: #{Db}"
require_relative '../../../../libs/ruby/mongo_tools'
puts "MongoTools: #{MongoTools}"

puts "-------------------------------------"
puts "common/push/after/import-quiz/run.rb"
puts "-------------------------------------"

raise "no config file specified" if ARGV[0] == nil

config = JSON.parse(IO.read(ARGV[0]))

puts "_____________XXXXxx________Xxx_______________________"


target_db_uri = config["ENV_MONGO_URI"]
raise "no target db specified" if target_db_uri == nil