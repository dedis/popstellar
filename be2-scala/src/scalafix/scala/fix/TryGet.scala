/*
rule = TryGet
*/
package fix

import scalafix.lint.LintSeverity
import scalafix.v1._
import scala.meta._

class TryGet extends SemanticRule("TryGet") {

  private def diag(pos: Position) = Diagnostic("", "Use of Try.Get", pos, "Using Try.get defeats the purpose of using Try in the first place. Use the following instead: Try.getOrElse, Try.fold, pattern matching or don't take the value out of the container and map over it to transform it", LintSeverity.Error)

  override def fix(implicit doc: SemanticDocument): Patch = {
    doc.tree.collect {
      // Corresponds to Try{_}.get or t.get
      case t @ Term.Select(qual, Term.Name("get")) if Util.matchType(qual, "scala/util/Try") => Patch.lint(diag(t.pos))
      case _ => Patch.empty
    }.asPatch
  }
}
