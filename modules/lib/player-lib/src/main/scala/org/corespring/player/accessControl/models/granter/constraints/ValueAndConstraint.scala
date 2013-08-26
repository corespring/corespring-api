package org.corespring.player.accessControl.models.granter.constraints

case class ValueAndConstraint[T](name: String, v: T, constraints: List[Constraint[T]]) {

  def valueWithinConstraints: Boolean = {
    constraints.foldRight(true)((c: Constraint[Any], acc: Boolean) => acc && c.allow(v))
  }


  /** Create a copy with only the failing constraints */
  def failingConstraints : ValueAndConstraint[T] = {
    this.copy(constraints = this.constraints.filterNot( c => c.allow(v) ) )
  }

  override def toString: String = "[" + name + ":" + v + " | constraints: " + constraints.mkString(",\n") + "]"
}

