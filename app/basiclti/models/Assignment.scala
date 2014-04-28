package basiclti.models

import org.bson.types.ObjectId

/**
 * Connects an LTI Assignment with a corespring ItemSession
 */
case class Assignment(
  /**
   * Uniquely identifies the student's interaction with the item session.
   */
  resultSourcedId: String,
  itemSessionId: ObjectId,
  gradePassbackUrl: String,
  onFinishedUrl: String)

