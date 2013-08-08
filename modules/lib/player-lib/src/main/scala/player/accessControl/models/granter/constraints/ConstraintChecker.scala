package player.accessControl.models.granter.constraints

abstract class ConstraintChecker {

  /** Return a list of failed constraints from the inputted constraints */
  protected final def failedConstraints(constraints:List[ValueAndConstraint[Any]]) : List[ValueAndConstraint[Any]] = {

    def fold(vc: ValueAndConstraint[Any], acc: List[ValueAndConstraint[Any]]): List[ValueAndConstraint[Any]] = {
      if (vc.valueWithinConstraints)
        acc
      else
        vc.failingConstraints :: acc
    }
    val failedConstraints = constraints.foldRight[List[ValueAndConstraint[Any]]](List())(fold)
    failedConstraints
  }


  protected def failed(name: String, msg: String): ValueAndConstraint[Any] = ValueAndConstraint(name, "Abstract ?", List(new FailedConstraint(msg)))

}
