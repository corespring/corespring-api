package org.corespring.platform.core.caching

import spray.caching.{ Cache, LruCache }

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

trait LocalCache[A] {
  def get(key: String): Option[A]
  def set(key: String, value: A): Unit
}

trait SimpleCache[A] extends LocalCache[A] {

  import scala.concurrent.ExecutionContext.Implicits.global

  def timeToLiveInMinutes: Double = 3

  private val c: Cache[A] = LruCache(timeToLive = timeToLiveInMinutes.minutes)

  override def get(key: String): Option[A] = {
    val r: Option[Future[A]] = c.get(key)
    r.map { f: Future[A] => Await.result(f, 1.second) }
  }

  override def set(key: String, value: A): Unit = c(key, () => Future(value))

}