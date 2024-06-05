/*
rule = TryGet
*/
package fix

import scalafix.lint.LintSeverity
import scalafix.v1._
import scala.meta._

case class TryGetDiag(tree: Tree) extends Diagnostic {
  override def message: String = "Use of Try.Get"

  override def severity: LintSeverity = LintSeverity.Error

  override def explanation: String = "Using Try.get is unsafe because it will throw the underlying exception in case of a Failure. Use the following instead: Try.getOrElse, Try.fold, pattern matching or don't take the value out of the container and map over it to transform it."

  override def position: Position = tree.pos
}

class TryGet extends SemanticRule("TryGet") {

  override def fix(implicit doc: SemanticDocument): Patch = {
    doc.tree.collect {
      // Corresponds to Try{_}.get or t.get
      case t @ Term.Select(qual, Term.Name("get")) if Util.matchType(qual, "scala/util/Try") => Patch.lint(TryGetDiag(t))
      case _ => Patch.empty
    }.asPatch
  }
}
