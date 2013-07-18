#!/usr/bin/env ruby

# => Copies all production assets s3 bucket -> the target env assets bucket.
# TODO: don't hardcode the production bucket 'corespring-assets'
#
require 'json'
config = JSON.parse(IO.read(ARGV[0]))

puts "Copy s3 bucket"

`which s3cmd`
raise "You need to install s3cmd: https://github.com/pearltrees/s3cmd-modification and have it on the path" unless $?.to_i == 0

target_bucket = config["ENV_AMAZON_ASSETS_BUCKET"]
raise "no amazon bucket specified in the config" if target_bucket.nil?

live_bucket = env["CORESPRING_LIVE_ASSETS_BUCKET"]
raise "no live bucket specified" if live_bucket.nil?

## Synch the live bucket with the target env bucket
## Remove any assets from the target bucket that aren't in the live bucket
cmd = "s3cmd sync -r --parallel --workers 100 --delete-removed s3://#{live_bucket}/ s3://#{target_bucket}/"

puts "Command: [#{cmd}]"
`#{cmd}`

raise "an error occurred running this script: #{cmd}" unless $?.to_i == 0
