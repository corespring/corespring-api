#!/usr/bin/env ruby

# => Copies all production assets s3 bucket -> the target env assets bucket.
# TODO: don't hardcode the production bucket 'corespring-assets'
#

def run(cmd)
  IO.popen(cmd) do |io|
    while line = io.gets
      print "[s3-copy] #{line}\n" unless line.empty?
    end
    io.close
    raise "An error occured" if $?.to_i != 0
  end
end

require 'json'
config = JSON.parse(IO.read(ARGV[0]))

puts "Copy s3 bucket"

`which s3cmd`
raise "You need to install s3cmd: https://github.com/pearltrees/s3cmd-modification and have it on the path" unless $?.to_i == 0

`which cs-api-assets`
raise "You need to install cs-api-assets (git@github.com:corespring/corespring-api-assets.git) and have it on the path" unless $?.to_i == 0

target_bucket = config["ENV_AMAZON_ASSETS_BUCKET"]
raise "no amazon bucket specified in the config" if target_bucket.nil?

live_bucket = ENV["CORESPRING_LIVE_ASSETS_BUCKET"]
raise "no live bucket specified" if live_bucket.nil?

`cd ~/cs-api-assets`

pull = "cs-api-assets pull-bucket #{live_bucket}"
push = "cs-api-assets push-bucket #{live_bucket} --remote-bucket=#{target_bucket}"

run(pull)
run(push)

`cd -`


