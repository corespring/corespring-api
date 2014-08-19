package org.corespring.platform.core.caching

import spray.caching.{ Cache, LruCache }

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

trait LocalCache[A] {
  def get(key: String): Option[A]
  def set(key: String, value: A): Boolean
}

trait SimpleCache[A] extends LocalCache[A] {

  import scala.concurrent.ExecutionContext.Implicits.global

  private val c: Cache[A] = LruCache(timeToLive = 2.minute, timeToIdle = 1.minute)

  override def get(key: String): Option[A] = {
    val r: Option[Future[A]] = c.get(key)
    r.map { f: Future[A] => Await.result(f, 1.second) }
  }

  override def set(key: String, value: A): Boolean = {
    c.apply(key, () => Future(value))
    true
  }
}