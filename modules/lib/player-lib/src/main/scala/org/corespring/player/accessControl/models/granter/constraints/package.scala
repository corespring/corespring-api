package org.corespring.player.accessControl.models.granter

import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId


package object constraints {

  /** An abstraction of whether a value is allowed */

  trait Constraint[+T] {
    def allow[U >: T](value: U): Boolean

    def describe: String = "some constraint with no description"
  }

  class WildcardConstraint extends Constraint[String] {
    def allow[U >: String](value: U): Boolean = true

    override def describe = "wildcard"
  }

  class StringEqualsConstraint(string: String) extends Constraint[String] {
    def allow[U >: String](value: U): Boolean = value == string

    override def toString: String = describe

    override def describe = "? == " + string
  }

  class AnyTimeConstraint extends Constraint[Long] {
    def allow[U >: Long](value: U) = true
  }

  abstract class NumberLessThanConstraint extends Constraint[Long] {
    def limit: Long

    def allow[U >: Long](value: U) = value.asInstanceOf[Long] < limit

    override def describe = "? < " + limit
  }

  abstract class NumberGreaterThanConstraint extends Constraint[Long] {
    def limit: Long

    def allow[U >: Long](value: U) = value.asInstanceOf[Long] > limit

    override def describe = "? > " + limit
  }


  class LongLessThan(l: Long) extends NumberLessThanConstraint {
    def limit = l

    override def toString: String = describe
  }

  class TimeExpiredConstraint(expiry:Long) extends NumberLessThanConstraint {
    def limit = expiry
    override def toString: String = describe
  }

  class SessionContainsItemId(itemId: VersionedId[ObjectId], lookup: SessionItemLookup) extends Constraint[ObjectId] {
    def allow[U >: ObjectId](value: U) = lookup.containsItem(value.asInstanceOf[ObjectId], itemId)

    override def toString: String = describe

    override def describe = "session-contains-item? " + itemId + " lookup: " + lookup
  }

  class LookupContainsItemId(itemId:VersionedId[ObjectId], lookup : ItemLookup) extends Constraint[ObjectId]{
    def allow[U >: ObjectId](value: U) = lookup.containsItem(value.asInstanceOf[ObjectId], itemId)

    override def toString: String = describe

    override def describe = "lookup-contains-item? " + itemId + " lookup: " + lookup
  }

  class FailedConstraint(msg: String) extends Constraint[Any] {
    def allow[U >: Any](value: U) = false

    override def toString: String = describe

    override def describe = "Failed: " + msg
  }


  class ObjectIdMatches(oid: ObjectId) extends Constraint[ObjectId] {

    def allow[U >: ObjectId](value: U) = value.asInstanceOf[ObjectId] == oid

    override def describe = "? == " + oid

    override def toString = describe
  }

  class VersionedIdMatches(id: VersionedId[ObjectId]) extends Constraint[VersionedId[ObjectId]] {

    def allow[U >: VersionedId[ObjectId]](value: U) = value.asInstanceOf[VersionedId[ObjectId]] == id

    override def describe = "[VersionedId] ? == " + id

    override def toString = describe
  }
}
