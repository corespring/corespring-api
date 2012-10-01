
require 'aws/s3'
require 'pp'

class AmazonUploader

  def initialize(access_key, secret_key, bucket)
    AWS::S3::Base.establish_connection!(
      :access_key_id     => access_key,
      :secret_access_key => secret_key
    )
    @bucket = bucket
    @s3_bucket = AWS::S3::Bucket.find(@bucket)
  end

  def store( file_path, s3_name, force = false )
    puts "AmazonUploader:store: #{file_path} -> #{s3_name}"

    if(!File.exists?(file_path))
      raise "file doesn't exist: #{file_path}"
    end


    if( local_file_newer_than_stored_file?(file_path, s3_name) || force )
      AWS::S3::S3Object.store( s3_name, open(file_path), @bucket)
    else 
      puts "not uploading file: #{file_path} - the uploaded file is more recent - to override this add force param"
    end
  end

  def local_file_newer_than_stored_file?(file_path, s3_name)
     result = @s3_bucket[s3_name]
     
     if( result.nil? )
      true
    else

      s3_last_modified = result.about['last-modified']
      s3_last_modified_time = Time.parse( s3_last_modified )

      local_file_modified = File.mtime(file_path)

      diff = local_file_modified - s3_last_modified_time 
      diff > 0
    end
  end


  def exists?( s3_name )
     result = s3_bucket[s3_name]
     #puts result
     #pp result.about
     puts "last uploaded: #{result.about['last-modified']}"
     !result.nil?
  end


end
