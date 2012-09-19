
class AmazonUploader

  def initialize(access_key, secret_key, bucket)
    AWS::S3::Base.establish_connection!(
      :access_key_id     => access_key,
      :secret_access_key => secret_key
    )
    @bucket = bucket
  end

  def store( file_path, s3_name )
    AWS::S3::S3Object.store( s3_name, open(file_path), @bucket)
    bucket = AWS::S3::Bucket.find(@bucket)
    result = bucket[s3_name]
    result
  end

end
