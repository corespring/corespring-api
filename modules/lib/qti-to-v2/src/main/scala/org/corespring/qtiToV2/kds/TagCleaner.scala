package org.corespring.qtiToV2.kds

import scala.xml._

object TagCleaner {

  def clean(node: Node): String = serialize(node, minimizeTags = MinimizeMode.Never).toString


  /**
   * Overridden xml.Utility.serialize for Scala < 2.11.5. Please remove if we upgrade. See
   * https://issues.scala-lang.org/browse/SI-8834 for more details
   */
  def serialize(
                 x: Node,
                 pscope: NamespaceBinding = TopScope,
                 sb: StringBuilder = new StringBuilder,
                 stripComments: Boolean = false,
                 decodeEntities: Boolean = true,
                 preserveWhitespace: Boolean = false,
                 minimizeTags: MinimizeMode.Value = MinimizeMode.Default): StringBuilder =
  {
    x match {
      case c: Comment if !stripComments => c buildString sb
      case s: SpecialNode               => s buildString sb
      case g: Group                     => for (c <- g.nodes) serialize(c, g.scope, sb, minimizeTags = minimizeTags) ; sb
      case el: Elem  =>
        // print tag with namespace declarations
        sb.append('<')
        el.nameToString(sb)
        if (el.attributes ne null) el.attributes.buildString(sb)
        el.scope.buildString(sb, pscope)
        if (el.child.isEmpty &&
          (minimizeTags == MinimizeMode.Always ||
            (minimizeTags == MinimizeMode.Default && el.minimizeEmpty)))
        {
          // no children, so use short form: <xyz .../>
          sb.append("/>")
        } else {
          // children, so use long form: <xyz ...>...</xyz>
          sb.append('>')
          sequenceToXML(el.child, el.scope, sb, stripComments, minimizeTags = minimizeTags)
          sb.append("</")
          el.nameToString(sb)
          sb.append('>')
        }
      case _ => throw new IllegalArgumentException("Don't know how to serialize a " + x.getClass.getName)
    }
  }

  def sequenceToXML(
                     children: Seq[Node],
                     pscope: NamespaceBinding = TopScope,
                     sb: StringBuilder = new StringBuilder,
                     stripComments: Boolean = false,
                     decodeEntities: Boolean = true,
                     preserveWhitespace: Boolean = false,
                     minimizeTags: MinimizeMode.Value = MinimizeMode.Default): Unit =
  {
    if (children.isEmpty) return
    else if (children forall isAtomAndNotText) { // add space
    val it = children.iterator
      val f = it.next
      serialize(f, pscope, sb, stripComments, decodeEntities, preserveWhitespace, minimizeTags)
      while (it.hasNext) {
        val x = it.next
        sb.append(' ')
        serialize(x, pscope, sb, stripComments, decodeEntities, preserveWhitespace, minimizeTags)
      }
    }
    else children foreach { serialize(_, pscope, sb, stripComments, decodeEntities, preserveWhitespace, minimizeTags) }
  }

  def isAtomAndNotText(x: Node) = x.isAtom && !x.isInstanceOf[Text]

}
