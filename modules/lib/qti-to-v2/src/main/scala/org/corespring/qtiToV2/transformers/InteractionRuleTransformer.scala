package org.corespring.qtiToV2.transformers

import org.corespring.qtiToV2.interactions.InteractionTransformer

import scala.xml._

class InteractionRuleTransformer(rules: InteractionTransformer*) extends BasicInteractionRuleTransformer {

  override def transform(n: Node, manifest: Node = ItemTransformer.EmptyManifest): Seq[Node] =
    rules.foldLeft(super.transform(n, manifest)) { (res, rule) => rule.transform(res, manifest) }

}

class BasicInteractionRuleTransformer {
  protected def unchanged(n: Node, ns: Seq[Node]) =
    ns.length == 1 && (ns.head == n)

  /**
   * Call transform(Node) for each node in ns, append results
   *  to NodeBuffer.
   */
  def transform(it: Iterator[Node], nb: NodeBuffer, manifest: Node): Seq[Node] =
    it.foldLeft(nb)(_ ++= transform(_, manifest)).toSeq

  /**
   * Call transform(Node) to each node in ns, yield ns if nothing changes,
   *  otherwise a new sequence of concatenated results.
   */
  def transform(ns: Seq[Node], manifest: Node): Seq[Node] = {
    val changed = ns flatMap (transform(_, manifest))
    if (changed.length != ns.length || (changed, ns).zipped.exists(_ != _)) changed
    else ns
  }

  def transform(n: Node, manifest: Node): Seq[Node] = {
    if (n.doTransform) n match {
      case Group(xs) => Group(transform(xs, manifest)) // un-group the hack Group tag
      case _ =>
        val ch = n.child
        val nch = transform(ch, manifest)

        if (ch eq nch) n
        else Elem(n.prefix, n.label, n.attributes, n.scope, nch.isEmpty, nch: _*)
    }
    else n
  }

  def apply(n: Node, manfiest: Node): Node = {
    val seq = transform(n, manfiest)
    if (seq.length > 1)
      throw new UnsupportedOperationException("transform must return single node for root")
    else seq.head
  }

}
