package basiclti.export

import xml.Elem

case class CCAssignmentGroups(groups:Seq[CCAssignmentGroup]) {
  def toXml:Elem = {
    val outer = <assignmentGroups xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                  xmlns="http://canvas.instructure.com/xsd/cccv1p0"
                                  xsi:schemaLocation="http://canvas.instructure.com/xsd/cccv1p0 http://canvas.instructure.com/xsd/cccv1p0.xsd">
                </assignmentGroups>
    new Elem(outer.prefix, outer.label, outer.attributes, outer.scope, (outer.child ++ groups.map(_.toXml)) : _*)
  }
}

case class CCAssignmentGroup(identifier: String, title: String){
  def toXml:Elem = <assignmentGroup identifier={identifier}>
    <title>{title}</title>
    <position>1</position>
    <group_weight>0</group_weight>
  </assignmentGroup>
}

case class CCExternalToolAssignmentSettings(identifier:String,title:String, assignmentGroupId:String, externalToolUrl:String, maxPoints:String = "10"){
  def toXml:Elem = <assignment identifier={identifier} xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://canvas.instructure.com/xsd/cccv1p0" xsi:schemaLocation="http://canvas.instructure.com/xsd/cccv1p0 http://canvas.instructure.com/xsd/cccv1p0.xsd">
    <title>{title}</title>
    <assignment_group_identifierref>{assignmentGroupId}</assignment_group_identifierref>
    <points_possible>{maxPoints}</points_possible>
    <grading_type>points</grading_type>
    <all_day>false</all_day>
    <submission_types>external_tool</submission_types>
    <position>1</position>
    <peer_reviews_assigned>false</peer_reviews_assigned>
    <peer_reviews>false</peer_reviews>
    <automatic_peer_reviews>false</automatic_peer_reviews>
    <grade_group_students_individually>false</grade_group_students_individually>
    <external_tool_url>{externalToolUrl}</external_tool_url>
    <external_tool_new_tab>false</external_tool_new_tab>
  </assignment>
}
case class CCHtmlAssignment(title:String){
  override def toString:String = "<html>\n<head>\n<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n<title>%s</title>\n</head>\n<body>\n\n</body>\n</html>".format(title);
}