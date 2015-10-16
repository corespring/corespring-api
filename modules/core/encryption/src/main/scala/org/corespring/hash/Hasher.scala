package org.corespring.hash

import org.mindrot.jbcrypt._

trait Hasher {

  def hash(plain: String): Hashed

  def matches(hashed: Hashed, plain: String): Boolean
}

case class Hashed(plain: String, hashed: String)

/**
 * This hasher will function the same as the SecureSocial hasher, so you can use this to hash passwords for SecureSocial.
 * If we ever move away from SecureSocial we'll want to make sure that we control the hashing logic. So we'd need to configure
 * whichever auth framework we use to use our hasher.
 */
class BCryptHasher extends Hasher {

  val DefaultRounds = 10

  def hash(plain: String): Hashed = Hashed(plain, BCrypt.hashpw(plain, BCrypt.gensalt(DefaultRounds)))

  def matches(hashed: Hashed, plain: String): Boolean = BCrypt.checkpw(plain, hashed.hashed)
}
