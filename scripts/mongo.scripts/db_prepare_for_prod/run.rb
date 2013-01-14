#!/bin/ruby
db = ARGV[0]
username = ARGV[1]
password = ARGV[2]

Dir["./*.js"].each do |f|
  puts f
  cmd = "mongo #{db} #{f} --eval \"var username='#{username}'; var password='#{password}'\""
  puts cmd
  `#{cmd}`
end